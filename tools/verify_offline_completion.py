"""Finish offline assertions after a Windows inherited-pipe wait in the QA launcher."""
from emulator_workflow import *
import sqlite3

if __name__=='__main__':
    assert (EVIDENCE/'10-offline-pending.png').exists()
    snapshot=fresh();records=[b for b in snapshot['batches'] if b['name']=='Offline test collection']
    assert len(records)==1 and records[0]['total']==3
    groups=[g for g in snapshot['groups'] if g['batch_id']==records[0]['id']]
    assert len(groups)==1 and groups[0]['location']=='C1-2' and groups[0]['quantity']==3
    shell('am','force-stop','com.capstone.balut')
    cache=PRIVATE/'qa-cache-proof.sqlite3';cache.write_bytes(adb('exec-out','run-as','com.capstone.balut','cat','databases/balut_cache.db'))
    with sqlite3.connect(cache) as db:assert db.execute('SELECT COUNT(*) FROM outbox').fetchone()[0]==0
    shell('am','start','-n','com.capstone.balut/.MainActivity');time.sleep(2)
    tap('Batches tab');fill_field('Search name, batch ID or source','Offline test collection');assert_text('Offline test collection');screenshot('11-offline-synchronized')
    result={'offline_banner_verified':True,'pending_registration_saved':True,'automatic_sync_after_reconnection':True,'exactly_one_server_record':True,'empty_outbox_verified_from_device_database':True,'registered_quantity':3,'mode':'simulation',
      'qa_note':'The initial QA script verified the offline steps, then waited on a Windows launcher output pipe after starting the server. The remaining server/UI/outbox assertions were completed by verify_offline_completion.py.'}
    (EVIDENCE/'emulator-offline.json').write_text(json.dumps(result,indent=2));print(json.dumps(result,indent=2));tap('Home tab')
