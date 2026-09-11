import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createWorkbenchState } from './workbenchState.js'

const geo = JSON.parse(readFileSync(new URL('../assets/maps/shandong.json', import.meta.url), 'utf8'))
const sql = readFileSync(new URL('../../../database/data.sql', import.meta.url), 'utf8')
const cities = [...sql.matchAll(/\((\d+), '([A-Z]+)', '([^']+)', ([\d.]+), ([\d.]+)\)/g)]
  .map(([, id, cityCode, cityName, longitude, latitude]) => ({ id: +id, cityCode, cityName, longitude: +longitude, latitude: +latitude }))
const models = [{ id: 9, modelCode: 'NOAA', modelName: 'NOAA', description: null }, { id: 42, modelCode: 'ECMWF', modelName: 'ECMWF', description: null }]
const elements = [{ id: 5, elementCode: 'PRECIP', elementName: '降水量', unit: 'mm' }, { id: 17, elementCode: 'T2M', elementName: '2 米气温', unit: '℃' }]
const times = ['2026-09-07 08:00:00', '2026-09-07 11:00:00', '2026-09-07 14:00:00']
const payload = (params) => ({
  model: models.find(m => m.id === params.modelId), element: elements.find(e => e.id === params.elementId), times,
  records: times.flatMap((forecastTime, index) => cities.map(city => ({ cityId: city.id, cityName: city.cityName, forecastTime, value: [19, 20.2, 21.1][index] })))
})
function setup(fetchWorkbench = async params => payload(params)) {
  const requests = []
  const api = {
    fetchCities: async () => cities, fetchForecastModels: async () => models, fetchWeatherElements: async () => elements,
    fetchWorkbench: params => { requests.push({ ...params }); return fetchWorkbench(params) }
  }
  return { ...createWorkbenchState(api, geo), requests, api }
}
const deferred = () => { let resolve, reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no }); return { promise, resolve, reject } }

test('monthly workbench keeps all six elements and queries each new layer', async () => {
  const extra = [['TCC','%'],['WIND_SPEED_100M','m/s'],['WIND_DIR_100M','°'],['RH','%']].map(([elementCode,unit],i)=>({id:100+i,elementCode,unit,elementName:elementCode}))
  const app=setup(params=>({...payload(params),element:[...elements,...extra].find(e=>e.id===params.elementId)}))
  app.api.fetchWeatherElements=async()=>[...elements,...extra]
  await app.initialize()
  assert.equal(app.state.elements.length,6)
  for(const element of extra) { await app.load({elementId:element.id}); assert.equal(app.state.status,'success'); assert.equal(app.state.response.element.unit,element.unit) }
})

test('initializes dictionaries, resolves defaults by code, sends all four parameters once', async () => {
  const app = setup()
  await app.initialize()
  assert.equal(app.state.status, 'success')
  assert.deepEqual(app.requests, [{ modelId: 42, elementId: 17, startTime: '2026-09-01 02:00:00', endTime: '2026-09-30 23:00:00' }])
  assert.equal(app.state.selectedTime, times[0])
  assert.equal(app.state.response.records.length, 48)
  assert.equal(app.state.matched, 16)
})
test('timeline is local, valid selections persist and unknown time is ignored', async () => {
  const app = setup(); await app.initialize()
  app.selectTime(times[1]); assert.equal(app.state.selectedTime, times[1])
  app.selectTime('invalid'); assert.equal(app.state.selectedTime, times[1])
  app.selectTime(times[2]); assert.equal(app.requests.length, 1)
})

test('changing model or element retains a valid selected forecast time', async () => {
  const app = setup(); await app.initialize()
  app.selectTime(times[2])
  await app.load({ modelId: 9 })
  assert.equal(app.state.selectedTime, times[2])
  await app.load({ elementId: 5 })
  assert.equal(app.state.selectedTime, times[2])
  await app.load({ modelId: 42, elementId: 17 })
  assert.equal(app.state.selectedTime, times[2])
})

test('rapid layer changes preserve the selected time even while the prior response is cleared', async () => {
  const pending = deferred()
  const app = setup(params => params.modelId === 9 ? pending.promise : payload(params))
  await app.initialize(); app.selectTime(times[2])
  const older = app.load({ modelId: 9 })
  await app.load({ modelId: 42, elementId: 5 })
  assert.equal(app.state.selectedTime, times[2])
  pending.resolve(payload({ modelId: 9, elementId: 17 })); await older
  assert.equal(app.state.selectedTime, times[2])
})

test('retry keeps a chosen time, but a changed range falls back to a real available time', async () => {
  let fail = false
  let availableTimes = times
  const app = setup(params => {
    if (fail) throw new Error('network')
    return { ...payload(params), times: availableTimes }
  })
  await app.initialize(); app.selectTime(times[2])
  fail = true; await app.load({ modelId: 9 })
  assert.equal(app.state.status, 'error')
  assert.equal(app.state.selectedTime, '')
  fail = false; await app.load()
  assert.equal(app.state.selectedTime, times[2])
  availableTimes = ['2026-09-08 05:00:00']
  await app.load({ range: ['2026-09-08 02:00:00', '2026-09-08 23:00:00'] })
  assert.equal(app.state.selectedTime, availableTimes[0])
})

for (const resource of ['city', 'model', 'element']) {
  test(`management-added ${resource} stays outside workbench core dictionaries after re-entry`, async () => {
    const app = setup()
    if (resource === 'city') app.api.fetchCities = async () => [...cities, { id: 99, cityCode: 'TMP_CITY', cityName: '临时城市', longitude: 117, latitude: 36 }]
    if (resource === 'model') app.api.fetchForecastModels = async () => [...models, { id: 99, modelCode: 'TMP_MODEL', modelName: '临时模型', description: null }]
    if (resource === 'element') app.api.fetchWeatherElements = async () => [...elements, { id: 99, elementCode: 'TMP_ELEMENT', elementName: '临时要素', unit: 'mm' }]
    await app.initialize()
    assert.equal(app.state.status, 'success')
    assert.equal(app.state.cities.length, 16)
    assert.equal(app.state.matched, 16)
    assert.deepEqual(app.state.models.map(item => item.modelCode).sort(), ['ECMWF', 'NOAA'])
    assert.deepEqual(app.state.elements.map(item => item.elementCode).sort(), ['PRECIP', 'T2M'])
    assert.equal(app.state.response.records.length, 48)
    assert.equal(app.requests.length, 1)
  })
}

test('filtering temporary cities does not hide a missing core city', async () => {
  const app = setup()
  app.api.fetchCities = async () => [...cities.slice(1), { id: 99, cityCode: 'TMP_CITY', cityName: '临时城市', longitude: 117, latitude: 36 }]
  await app.initialize()
  assert.equal(app.state.status, 'error')
  assert.equal(app.state.dictionariesReady, false)
  assert.equal(app.requests.length, 0)
})
test('returning to the same model/element/range consumes cache without another request', async () => {
  const app = setup(); await app.initialize()
  await app.load({ elementId: 5 }); await app.load({ elementId: 17 })
  assert.equal(app.state.source, 'cache')
  assert.equal(app.state.response.element.unit, '℃')
  assert.equal(app.requests.length, 2)
})
test('late old success cannot overwrite a newer cache hit', async () => {
  const pending = deferred()
  const app = setup(params => params.modelId === 9 ? pending.promise : Promise.resolve(payload(params)))
  await app.initialize()
  const old = app.load({ modelId: 9 })
  assert.equal(app.state.status, 'loading')
  assert.equal(app.state.response, null)
  await app.load({ modelId: 42 })
  pending.resolve(payload({ modelId: 9, elementId: 17 })); await old
  assert.equal(app.state.response.model.modelCode, 'ECMWF')
  assert.equal(app.state.status, 'success')
})
test('late old error cannot erase the newest successful selection', async () => {
  const pending = deferred()
  const app = setup(params => params.modelId === 9 ? pending.promise : Promise.resolve(payload(params)))
  await app.initialize(); const old = app.load({ modelId: 9 }); await app.load({ modelId: 42 })
  pending.reject(new Error('server failed')); await old
  assert.equal(app.state.status, 'success')
  assert.equal(app.state.error, '')
})
test('successful empty range retains metadata and is not treated as an error', async () => {
  const app = setup(async params => ({ ...payload(params), times: [], records: [] }))
  await app.initialize()
  assert.equal(app.state.status, 'empty')
  assert.equal(app.state.selectedTime, '')
  assert.equal(app.state.response.model.modelCode, 'ECMWF')
})
test('network failure ends loading, is not cached, and retry recovers without exposing raw errors', async () => {
  let fail = true
  const app = setup(async params => { if (fail) throw new Error('sensitive server internals'); return payload(params) })
  await app.initialize()
  assert.equal(app.state.status, 'error')
  assert.doesNotMatch(app.state.error, /sensitive/)
  fail = false; await app.load()
  assert.equal(app.state.status, 'success'); assert.equal(app.requests.length, 2)
})
test('dictionary failure is retryable; workbench is never requested before dictionaries succeed', async () => {
  const app = setup(); app.api.fetchCities = async () => { throw new Error('offline') }
  await app.initialize(); assert.equal(app.state.status, 'error'); assert.equal(app.requests.length, 0)
  app.api.fetchCities = async () => cities
  await app.initialize(); assert.equal(app.state.status, 'success')
})
test('invalid range invalidates old data without making a request', async () => {
  const app = setup(); await app.initialize()
  await app.load({ range: [times[2], times[0]] })
  assert.equal(app.state.status, 'error'); assert.equal(app.state.response, null); assert.equal(app.requests.length, 1)
})
test('leaving the page invalidates in-flight work', async () => {
  const pending = deferred(); const app = setup(() => pending.promise)
  const work = app.initialize()
  await new Promise(resolve => setImmediate(resolve))
  app.dispose(); pending.resolve(payload({ modelId: 42, elementId: 17 })); await work
  assert.equal(app.state.response, null)
})
