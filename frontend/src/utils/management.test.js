import test from 'node:test'
import assert from 'node:assert/strict'
const m = await import('./management.js').catch(() => ({}))
test('management helpers exist for approved write contracts', () => assert.equal(typeof m.cityPayload, 'function'))
test('city payload preserves zero coordinates and rejects missing/precision/range', () => {
  assert.deepEqual(m.cityPayload({cityCode:'TMP_CITY',cityName:' Test ',longitude:'0',latitude:'0'}),{cityCode:'TMP_CITY',cityName:'Test',longitude:0,latitude:0})
  for(const longitude of ['',null,'NaN','Infinity','181','1.1234567']) assert.throws(()=>m.cityPayload({cityCode:'TMP',cityName:'Test',longitude,latitude:0}))
})
test('codes and names respect backend length and code pattern',()=>{
  for(const cityCode of ['lower','A'.repeat(33),'']) assert.throws(()=>m.cityPayload({cityCode,cityName:'Test',longitude:0,latitude:0}))
  assert.throws(()=>m.modelPayload({modelCode:'TMP',modelName:'x'.repeat(51)}))
  assert.throws(()=>m.elementPayload({elementCode:'TMP',elementName:'test',unit:'x'.repeat(17)}))
})
test('model cleared description becomes null and missing display is --',()=>{
  assert.deepEqual(m.modelPayload({modelCode:'TMP',modelName:'Test',description:''}),{modelCode:'TMP',modelName:'Test',description:null})
  assert.equal(m.displayDescription(null),'--'); assert.equal(m.displayDescription(''),'--')
  assert.throws(()=>m.modelPayload({modelCode:'TMP',modelName:'Test',description:'x'.repeat(256)}))
})
const record={cityId:8,modelId:41,elementId:71,forecastTime:'2026-09-07 08:00:00',value:'0.00'}
test('record uses exact local datetime, numeric IDs and real zero',()=>{
  assert.deepEqual(m.recordPayload(record,[{id:71,elementCode:'PRECIP'}]),{...record,value:0})
  for(const forecastTime of ['2026-09-07T08:00:00Z','2026-02-30 08:00:00','0999-09-07 08:00:00']) assert.throws(()=>m.recordPayload({...record,forecastTime},[{id:71,elementCode:'PRECIP'}]))
})
test('precipitation negative is rejected while T2M negative is legal',()=>{
  assert.throws(()=>m.recordPayload({...record,value:'-0.01'},[{id:71,elementCode:'PRECIP'}]))
  assert.equal(m.recordPayload({...record,value:'-20.25'},[{id:71,elementCode:'T2M'}]).value,-20.25)
  for(const value of ['0.001','100000000','',null,'NaN']) assert.throws(()=>m.recordPayload({...record,value},[{id:71,elementCode:'T2M'}]))
  for(const cityId of [0,8.2,'8',9007199254740992]) assert.throws(()=>m.recordPayload({...record,cityId},[{id:71,elementCode:'T2M'}]))
})
test('edit draft does not mutate table row',()=>{
  const row={id:8,cityName:'Original'};const draft=m.copyDraft(row);draft.cityName='Draft';assert.equal(row.cityName,'Original')
})
test('backend errors prefer safe message and never expose raw transport internals',()=>{
  assert.equal(m.managementError({response:{status:409,data:{message:'city is referenced'}}}),'city is referenced')
  for(const status of [400,404,409,500]) assert.ok(m.managementError({response:{status,data:{}}}))
  assert.equal(m.managementError(new Error('private Axios object')),'无法连接后端服务')
  assert.equal(m.managementError({response:{status:500,data:{message:'<html>SQL password</html>'}}}),'服务器处理失败')
})
