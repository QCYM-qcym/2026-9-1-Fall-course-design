import test from 'node:test'
import assert from 'node:assert/strict'
const m=await import('./managementState.js').catch(()=>({}))
const deferred=()=>{let resolve,reject;const promise=new Promise((a,b)=>{resolve=a;reject=b});return{promise,resolve,reject}}
test('management list state exposes load and dispose',()=>assert.equal(typeof m.createManagementList,'function'))
test('list loading, empty, failure and retry are distinct',async()=>{
 let fail=false;let rows=[];const s=m.createManagementList(async()=>{if(fail)throw Error('offline');return rows})
 const p=s.load();assert.equal(s.state.status,'loading');await p;assert.equal(s.state.status,'empty')
 fail=true;await s.load();assert.equal(s.state.status,'error');fail=false;rows=[{id:1}];await s.load();assert.equal(s.state.status,'success')
})
test('old success/failure cannot overwrite current list; dispose invalidates pending work',async()=>{
 const a=deferred(),b=deferred(),c=deferred();const queue=[a,b,c];const s=m.createManagementList(()=>queue.shift().promise)
 const first=s.load(),second=s.load();b.resolve([{id:2}]);await second;a.reject(Error('old'));await first;assert.equal(s.state.rows[0].id,2)
 const last=s.load();s.dispose();c.resolve([{id:3}]);await last;assert.equal(s.state.rows[0].id,2)
})
test('write success plus refresh failure is not write failure and prevents duplicate submission',async()=>{
 const d=deferred();let writes=0;const s=m.createManagementList(async()=>{throw Error('GET failed')})
 const first=s.write(()=>{writes++;return d.promise});const duplicate=await s.write(()=>{writes++})
 assert.equal(duplicate,null);d.resolve();const result=await first;assert.equal(writes,1);assert.deepEqual(result,{saved:true,refreshed:false});assert.equal(s.state.busy,false)
})
test('write failure keeps current rows and does not fetch',async()=>{
 let calls=0;const s=m.createManagementList(async()=>{calls++;return[{id:1}]});await s.load()
 await assert.rejects(s.write(async()=>{throw Error('conflict')}));assert.equal(calls,1);assert.equal(s.state.rows[0].id,1)
})

test('failed write does not strand an in-flight list in loading',async()=>{
 const pending=deferred();const s=m.createManagementList(()=>pending.promise)
 const loading=s.load()
 await assert.rejects(s.write(async()=>{throw Error('conflict')}))
 pending.resolve([{id:1}]);await loading
 assert.equal(s.state.status,'success')
})

test('dispose during post-write refresh suppresses caller UI updates',async()=>{
 const pending=deferred();const s=m.createManagementList(()=>pending.promise)
 const writing=s.write(async()=>{})
 await Promise.resolve();s.dispose();pending.resolve([])
 assert.equal(await writing,null)
})
