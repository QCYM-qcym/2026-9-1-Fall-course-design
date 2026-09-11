import test from 'node:test'
import assert from 'node:assert/strict'
import http from '../api/http.js'
const api = await import('../api/management.js').catch(()=>({}))
const csrfResponse = config => ({data:{code:200,data:{token:'test-csrf',headerName:'X-XSRF-TOKEN',parameterName:'_csrf'}},status:200,config})
test('management uses existing Axios and exact resource URL/method/body',async()=>{
  const original=http.defaults.adapter; const calls=[]
  http.defaults.adapter=async config=>{if(config.url==='/auth/csrf') return csrfResponse(config);calls.push(config);return {data:{code:200,data:config.method==='get'?[]:config.method==='delete'?null:{id:8}},status:200,config}}
  try {
    for(const [resource,path] of [['City','cities'],['ForecastModel','forecast-models'],['WeatherElement','weather-elements'],['ForecastRecord','forecast-records']]) {
      const body={name:'only submitted fields'}
      await api['create'+resource](body);await api['update'+resource](8,body);await api['delete'+resource](8)
      assert.deepEqual(calls.slice(-3).map(c=>[c.method,c.url]),[['post','/'+path],['put','/'+path+'/8'],['delete','/'+path+'/8']])
      assert.deepEqual(JSON.parse(calls.at(-2).data),body)
      assert.equal(calls.at(-2).headers.get('X-XSRF-TOKEN'),'test-csrf')
    }
    assert.deepEqual(await api.fetchForecastRecords(),[])
  } finally {http.defaults.adapter=original}
})
test('management keeps backend business error envelope for UI',async()=>{
  const original=http.defaults.adapter
  http.defaults.adapter=async config=>config.url==='/auth/csrf'?csrfResponse(config):({data:{code:409,message:'duplicate'},status:200,config})
  try {await assert.rejects(api.createCity({}),e=>e.response.data.message==='duplicate'&&e.response.status===409)} finally {http.defaults.adapter=original}
})
