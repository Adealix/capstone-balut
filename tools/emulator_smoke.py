"""Interact with this app in the Android Studio emulator using Android's ADB UI dump.
Local QA credentials are generated once in .local (ignored, excluded from deliverables).
Never prints passwords or session tokens.
"""
from pathlib import Path
import os,subprocess,time,json,re,secrets,string,urllib.request,xml.etree.ElementTree as ET
ROOT=Path('D:/capstone-balut');ADB=Path(os.environ['LOCALAPPDATA'])/'Android/Sdk/platform-tools/adb.exe'
EVIDENCE=ROOT/'evidence';PRIVATE=ROOT/'.local';PRIVATE.mkdir(exist_ok=True)
def adb(*args):return subprocess.run([str(ADB),'-s','emulator-5554',*args],capture_output=True,check=True).stdout
def shell(*args):return adb('shell',*args).decode('utf-8',errors='replace')
def tree():
    shell('uiautomator','dump','/sdcard/balut-window.xml')
    return ET.fromstring(adb('exec-out','cat','/sdcard/balut-window.xml'))
def nodes():return list(tree().iter('node'))
def tap_node(node):
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.attrib['bounds']));shell('input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.6)
def tap(text,scroll=False):
    for attempt in range(7 if scroll else 1):
        for node in sorted(nodes(),key=lambda n:n.attrib.get('clickable')!='true'):
            if node.attrib.get('text','').casefold()==text.casefold() or node.attrib.get('content-desc','').casefold()==text.casefold():
                if node.attrib.get('enabled')=='false':continue
                tap_node(node);return
        if scroll:shell('input','swipe','530','1900','530','650','350');time.sleep(.5)
    raise AssertionError('UI element not found: '+text)
def fill(node,value):
    tap_node(node);shell('input','keyevent','123')
    current=node.attrib.get('text','')
    if current:shell('input','keyevent',*(['67']*(len(current)+2)))
    shell('input','text',value.replace(' ','%s'))
def edits():return [n for n in nodes() if n.attrib.get('class')=='android.widget.EditText']
def fill_field(label,value):
    # Re-read bounds after each keyboard open/close; never reuse stale coordinates.
    for attempt in range(6):
        candidates=[n for n in nodes() if n.attrib.get('class')=='android.widget.EditText' and n.attrib.get('content-desc')==label]
        if candidates:
            fill(candidates[0],value);shell('input','keyevent','4');return
        root=tree();scrolls=[n for n in root.iter('node') if n.attrib.get('class')=='android.widget.ScrollView']
        if scrolls:
            x1,y1,x2,y2=map(int,re.findall(r'\d+',scrolls[-1].attrib['bounds']));x=(x1+x2)//2
            top=y1+40;bottom=y2-40
            shell('input','swipe',str(x),str(top if attempt<2 else bottom),str(x),str(bottom if attempt<2 else top),'180')
        time.sleep(.2)
    raise AssertionError('Field not found: '+label)
def screenshot(name):
    (EVIDENCE/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'))
    xml=adb('exec-out','cat','/sdcard/balut-window.xml');(EVIDENCE/(name+'.xml')).write_bytes(xml)
def assert_text(text):
    assert any(text in n.attrib.get('text','') or text in n.attrib.get('content-desc','') for n in nodes()),text
def api(path,body=None,token=None):
    req=urllib.request.Request('http://127.0.0.1:8787'+path,data=json.dumps(body).encode() if body is not None else None,headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})})
    return json.loads(urllib.request.urlopen(req,timeout=15).read())
def run():
    apk=ROOT/'frontend/app/build/outputs/apk/debug/app-debug.apk';adb('install','-r',str(apk));shell('am','start','-n','com.capstone.balut/.MainActivity');time.sleep(4)
    health=api('/health');credentials=PRIVATE/'qa-login.json'
    if health['setup_required']:
        login={'username':'farmowner','password':''.join(secrets.choice(string.ascii_letters+string.digits) for _ in range(24)),'name':'Farm Owner'}
        credentials.write_text(json.dumps(login,indent=2))
        for label,value in [('Your name',login['name']),('Username',login['username']),('Password (at least 8 characters)',login['password'])]:fill_field(label,value)
        tap('Create Owner account',True);time.sleep(4)
    else:
        login=json.loads(credentials.read_text())
        if any(n.attrib.get('text')=='Sign in' for n in nodes()):
            fill_field('Username',login['username']);fill_field('Password',login['password']);tap('Sign in',True);time.sleep(3)
    for label in ('Not now','Allow'):
        try:tap(label)
        except AssertionError:pass
    token=api('/auth/login',{'username':login['username'],'password':login['password']})['token']
    if len(api('/snapshot',token=token)['batches'])==0:api('/demo/seed',{},token)
    shell('am','force-stop','com.capstone.balut');shell('am','start','-n','com.capstone.balut/.MainActivity');time.sleep(3)
    for label in ('Not now','Allow'):
        try:tap(label)
        except AssertionError:pass
    assert_text('Your farm, today.');screenshot('01-home')
    for label,expected,file in [('Dashboard tab','Incubator overview','02-dashboard'),('Scan tab','ML integration pending','03-scan'),('Batches tab','Your batches','04-batches'),('Profile tab','Your workspace','05-profile')]:
        tap(label);assert_text(expected);screenshot(file)
    tap('Batches tab');tap('View batch');assert_text('Egg groups & locations');screenshot('06-batch-details')
    shell('input','keyevent','4');tap('Home tab');assert_text('Your farm, today.')
    evidence={'apk_installed':True,'owner_setup_or_login':True,'session_survived_process_restart':True,'five_tabs_verified':True,'batch_details_opened':True,'mode':'simulation','credentials_logged':False}
    (EVIDENCE/'emulator-smoke.json').write_text(json.dumps(evidence,indent=2));print(json.dumps(evidence,indent=2))
if __name__=='__main__':run()
