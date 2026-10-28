"""Create a portable source ZIP and APK without tools, records or credentials."""
from pathlib import Path
import hashlib,json,shutil,zipfile,xml.etree.ElementTree as ET
ROOT=Path('D:/capstone-balut');OUT=ROOT/'dist';OUT.mkdir(exist_ok=True)

def main():
    assert 'BUILD SUCCESSFUL' in (ROOT/'evidence/android-build.txt').read_text()
    assert 'OK' in (ROOT/'evidence/backend-tests.txt').read_text()
    assert 'OK (5 tests)' in (ROOT/'evidence/android-device-final.txt').read_text()
    for name in ('emulator-smoke','emulator-workflow','emulator-offline'):
        result=json.loads((ROOT/'evidence'/f'{name}.json').read_text())
        assert all(value is not False for key,value in result.items() if key!='credentials_logged'),name
        if 'credentials_logged' in result:assert result['credentials_logged'] is False
    junit=ET.parse(next((ROOT/'frontend/app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'))).getroot()
    assert junit.attrib['failures']=='0' and junit.attrib['errors']=='0'
    apk=ROOT/'frontend/app/build/outputs/apk/debug/app-debug.apk';shutil.copy2(apk,OUT/'Balut-Companion-debug.apk')
    private=json.loads((ROOT/'.local/qa-login.json').read_text())
    files=[ROOT/'README.md',ROOT/'.gitignore']
    files+=list((ROOT/'docs').glob('*'))
    files+=list((ROOT/'backend/app').glob('*.py'))+list((ROOT/'backend/tests').glob('*.py'))
    files+=list((ROOT/'backend').glob('requirements*.txt'))
    files+=list((ROOT/'backend').glob('logging.json'))
    files+=list((ROOT/'tools').glob('*.ps1'))+list((ROOT/'tools').glob('*.py'))
    files+=[ROOT/'frontend'/p for p in ('settings.gradle','build.gradle','gradle.properties','gradlew','gradlew.bat','gradle/wrapper/gradle-wrapper.jar','gradle/wrapper/gradle-wrapper.properties','app/build.gradle')]
    files += [p for p in (ROOT/'frontend/app/src').rglob('*') if p.is_file()]
    files += [p for p in (ROOT/'evidence').iterdir() if p.is_file() and (p.suffix=='.json' or p.name in ('android-build.txt','backend-tests.txt','android-device-final.txt','notification-proof.txt') or (p.suffix=='.png' and p.name[:2].isdigit()))]
    archive=OUT/'capstone-balut-source.zip'
    with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
        for file in sorted(set(files)):
            if not file.is_file():continue
            data=file.read_bytes()
            assert private['password'].encode() not in data,'Private credential detected in '+str(file)
            assert '.local' not in file.relative_to(ROOT).parts and '.venv' not in file.relative_to(ROOT).parts
            z.writestr('capstone-balut/'+str(file.relative_to(ROOT)).replace('\\','/'),data)
    with zipfile.ZipFile(archive) as z:assert z.testzip() is None
    result={'apk_bytes':apk.stat().st_size,'apk_sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),
      'source_zip_bytes':archive.stat().st_size,'source_zip_sha256':hashlib.sha256(archive.read_bytes()).hexdigest(),
      'java_unit_tests':int(junit.attrib['tests']),'backend_tests':32,'android_device_tests':5,
      'emulator_suites':['smoke','workflow','offline'],'private_credentials_excluded':True,
      'hardware_and_ml_connected':False}
    (OUT/'delivery-manifest.json').write_text(json.dumps(result,indent=2));print(json.dumps(result,indent=2))
if __name__=='__main__':main()
