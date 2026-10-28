"""Exercise record-changing paths in the project's simulation emulator only."""
from emulator_smoke import *

def token():
    login=json.loads((PRIVATE/'qa-login.json').read_text())
    return api('/auth/login',{'username':login['username'],'password':login['password']})['token']
def fresh():return api('/snapshot',token=token())
def goto_batch(name):
    tap('Batches tab');fill_field('Search name, batch ID or source',name);tap('View batch',True);time.sleep(1)
def tap_in_card(marker,label):
    for attempt in range(8):
        root=tree()
        for node in reversed(list(root.iter('node'))):
            descendants=list(node.iter('node'))
            if any(marker in child.attrib.get('text','') for child in node.findall('node')):
                for child in descendants:
                    if child.attrib.get('text')==label:tap_node(child);return
        shell('input','swipe','530','1900','530','650','250')
    raise AssertionError('Could not find card action '+marker+' / '+label)
def run():
    s=fresh();batch=next(b for b in s['batches'] if b['name']=='River flock');bid=batch['id'];original=batch['started']
    shell('am','force-stop','com.capstone.balut');shell('am','start','-n','com.capstone.balut/.MainActivity');time.sleep(2);goto_batch('River flock')
    group=next(g for g in fresh()['groups'] if g['batch_id']==bid and g['quantity']>0)
    if group['location'].startswith('C1'):
        tap('Record transfer to C2',True);tap('Confirm transfer');time.sleep(2)
    assert next(g for g in fresh()['groups'] if g['id']==group['id'])['location']=='C2'
    assert next(b for b in fresh()['batches'] if b['id']==bid)['started']==original
    existing=[g for g in fresh()['groups'] if g['batch_id']==bid and g['location']=='C3-2']
    if not existing:
        tap('Review & assign finishing row',True);tap('Destination',True);tap('C3-2');tap('Confirm transfer');time.sleep(2)
    groups=[g for g in fresh()['groups'] if g['batch_id']==bid];assert sum(g['quantity'] for g in groups)==30
    finishing=next(g for g in groups if g['location']=='C3-2');assert finishing['quantity']==6
    screenshot('07-transfer-split')
    tap_in_card('C3-2','Record outcome / removal');fill_field('Egg count','2');tap('Actual outcome');tap('Removed');tap('Record');time.sleep(2)
    s=fresh();active=sum(g['quantity'] for g in s['groups'] if g['batch_id']==bid);done=sum(o['quantity'] for o in s['outcomes'] if o['batch_id']==bid)
    assert active==28 and active+done==30
    tap('Alerts');before=sum(g['quantity'] for g in s['groups']);tap('Acknowledge review',True);time.sleep(2);assert sum(g['quantity'] for g in fresh()['groups'])==before
    screenshot('08-alerts')
    # The first C1 position was freed by transfer, so register a real UI test collection.
    tap('Home tab');tap('Register a batch');fill_field('Batch name','Emulator test collection');fill_field('Egg source / supplier','QA simulation');fill_field('Number of eggs','4');tap('Save batch',True);time.sleep(2)
    assert len([b for b in fresh()['batches'] if b['name']=='Emulator test collection'])==1
    shell('am','force-stop','com.capstone.balut');shell('am','start','-n','com.capstone.balut/.MainActivity');time.sleep(2);tap('Batches tab');fill_field('Search name, batch ID or source','Emulator test collection');assert_text('Emulator test collection');screenshot('09-registration-persisted')
    tap('Profile tab');tap('Settings & connection');tap('Send test notification',True);tap('OK');
    own=[line for line in shell('dumpsys','notification','--noredact').splitlines() if 'Test review reminder' in line or 'com.capstone.balut' in line]
    assert any('Test review reminder' in line for line in own)
    (EVIDENCE/'notification-proof.txt').write_text('\n'.join(own))
    result={'manual_c1_to_c2':True,'original_date_retained':True,'split_to_c3_row':True,'partial_outcome_reconciles':True,'acknowledge_does_not_harvest':True,'ui_batch_registration':True,'registration_survives_restart':True,'test_notification_posted':True,'mode':'simulation'}
    (EVIDENCE/'emulator-workflow.json').write_text(json.dumps(result,indent=2));print(json.dumps(result,indent=2))
if __name__=='__main__':run()
