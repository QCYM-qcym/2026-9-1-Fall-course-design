import test from 'node:test'
import assert from 'node:assert/strict'
import * as comparison from './comparisonState.js'

const range = ['2026-09-07 08:00:00', '2026-09-07 14:00:00']
const cities = [{ id: 9, cityCode: 'QINGDAO', cityName: '青岛' }, { id: 8, cityCode: 'JINAN', cityName: '济南' }]
const elements = [{ id: 83, elementCode: 'PRECIP', elementName: '降水量', unit: 'mm' }, { id: 71, elementCode: 'T2M', elementName: '2 米气温', unit: '℃' }]
const response = (cityId = 8, elementId = 71) => ({ cityId, cityName: cities.find(c => c.id === cityId).cityName,
  element: elements.find(e => e.id === elementId), series: [
    { modelId: 41, modelCode: 'ECMWF', modelName: 'ECMWF', values: [{ forecastTime: range[0], value: 0 }] },
    { modelId: 59, modelCode: 'NOAA', modelName: 'NOAA', values: [{ forecastTime: range[0], value: 0.1 }] }] })
const deferred = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b }); return { promise, resolve, reject } }
function create(overrides = {}) {
  assert.equal(typeof comparison.createComparisonState, 'function', 'comparison state must be implemented')
  const calls = []
  const api = { fetchCities: async () => cities, fetchWeatherElements: async () => elements,
    fetchComparison: async params => { calls.push(params); return response(params.cityId, params.elementId) }, ...overrides }
  return { ...comparison.createComparisonState(api), calls }
}

test('comparison parallel dictionaries choose JINAN/T2M by code and auto query exactly once', async () => {
  const c = deferred(), e = deferred(); let cityCalled = false, elementCalled = false
  const model = create({ fetchCities: () => { cityCalled = true; return c.promise }, fetchWeatherElements: () => { elementCalled = true; return e.promise } })
  assert.equal(model.state.status, 'idle')
  const pending = model.initialize(); assert.equal(model.state.status, 'loading'); assert.ok(cityCalled && elementCalled)
  c.resolve(cities); e.resolve(elements); await pending
  assert.equal(model.state.cityId, 8); assert.equal(model.state.elementId, 71); assert.equal(model.state.status, 'success')
  assert.deepEqual(model.calls, [{ cityId: 8, elementId: 71, startTime: range[0], endTime: range[1] }])
})

test('comparison absent defaults fall back to first supported dictionary items', async () => {
  const m = create({ fetchCities: async () => [cities[0]], fetchWeatherElements: async () => [elements[0]] })
  await m.initialize(); assert.equal(m.state.cityId, 9); assert.equal(m.state.elementId, 83); assert.ok(m.state.warning)
})

test('monthly comparison can select and query all four additional elements', async () => {
  const extra=[['TCC','%'],['WIND_SPEED_100M','m/s'],['WIND_DIR_100M','°'],['RH','%']].map(([elementCode,unit],i)=>({id:100+i,elementCode,unit,elementName:elementCode}))
  const m=create({fetchWeatherElements:async()=>[...elements,...extra],fetchComparison:async p=>({...response(),element:[...elements,...extra].find(e=>e.id===p.elementId)})})
  await m.initialize(); assert.equal(m.state.elements.length,6)
  for(const e of extra) { m.state.elementId=e.id; await m.load(); assert.equal(m.state.status,'success'); assert.equal(m.state.response.element.unit,e.unit) }
})

test('comparison draft changes do not fetch; explicit queries have no cache', async () => {
  const m = create(); await m.initialize()
  m.state.cityId = 9; m.state.elementId = 83
  assert.equal(m.calls.length, 1); assert.equal(m.state.response.cityId, 8)
  await m.load(); await m.load(); assert.equal(m.calls.length, 3)
  assert.equal(m.state.response.cityId, 9); assert.equal(m.state.response.element.id, 83)
})

test('comparison loading clears old response and applied query until new success', async () => {
  let pending = null
  const m = create({ fetchComparison: async () => pending ? pending.promise : response() }); await m.initialize()
  pending = deferred(); const work = m.load()
  assert.equal(m.state.status, 'loading'); assert.equal(m.state.response, null); assert.equal(m.state.appliedQuery, null)
  pending.resolve(response()); await work; assert.equal(m.state.status, 'success')
})

test('comparison empty 200 remains distinct from single-model data and true zero', async () => {
  const data = response(); data.series[1].values = []
  const m = create({ fetchComparison: async () => structuredClone(data) }); await m.initialize()
  assert.equal(m.state.status, 'success'); assert.equal(m.state.response.series[0].values[0].value, 0)
  data.series[0].values = []; await m.load(); assert.equal(m.state.status, 'empty'); assert.equal(m.state.response.series.length, 2)
})

test('comparison error and recovery retry preserve all current filters', async () => {
  let fail = true; const calls = []
  const m = create({ fetchComparison: async p => { calls.push(p); if (fail) throw new Error('private SQL'); return response(p.cityId, p.elementId) } })
  await m.initialize(); assert.equal(m.state.status, 'error'); assert.doesNotMatch(m.state.error, /private SQL/)
  m.state.cityId = 9; m.state.elementId = 83; m.state.range = ['2026-09-08 08:00:00', '2026-09-08 14:00:00']
  fail = false; await m.retry()
  assert.equal(m.state.status, 'success')
  assert.deepEqual(calls.at(-1), { cityId: 9, elementId: 83, startTime: '2026-09-08 08:00:00', endTime: '2026-09-08 14:00:00' })
})

test('comparison failed dictionaries retry without resetting edited range', async () => {
  let fail = true
  const m = create({ fetchCities: async () => { if (fail) throw new Error('offline'); return cities } })
  await m.initialize(); assert.equal(m.state.status, 'error'); assert.equal(m.calls.length, 0)
  m.state.range = ['2026-09-08 08:00:00', '2026-09-08 14:00:00']; fail = false; await m.retry()
  assert.equal(m.state.status, 'success'); assert.equal(m.calls[0].startTime, '2026-09-08 08:00:00')
})

test('comparison unsupported dictionaries do not fabricate IDs or query', async () => {
  const m = create({ fetchCities: async () => [{ id: 99, cityCode: 'OTHER' }] })
  await m.initialize(); assert.equal(m.state.status, 'error'); assert.equal(m.calls.length, 0)
})

for (const oldFailure of [false, true]) {
  test(`comparison stale ${oldFailure ? 'failure' : 'success'} cannot overwrite latest response`, async () => {
    const queue = []; let initial = true
    const m = create({ fetchComparison: async () => { if (initial) return response(); const p = deferred(); queue.push(p); return p.promise } })
    await m.initialize(); initial = false
    const a = m.load(); m.state.cityId = 9; m.state.elementId = 83; const b = m.load()
    queue[1].resolve(response(9, 83)); await b
    if (oldFailure) queue[0].reject(new Error('old')); else queue[0].resolve(response())
    await a; assert.equal(m.state.status, 'success'); assert.equal(m.state.response.cityId, 9); assert.equal(m.state.response.element.id, 83)
  })
}

test('comparison invalid submission invalidates pending older request', async () => {
  let pending = null
  const m = create({ fetchComparison: async () => pending ? pending.promise : response() }); await m.initialize()
  pending = deferred(); const a = m.load(); m.state.range = []; await m.load()
  pending.resolve(response()); await a; assert.equal(m.state.status, 'error'); assert.equal(m.state.response, null)
})

test('comparison wrong response identity is rejected', async () => {
  const m = create({ fetchComparison: async () => response(9, 83) }); await m.initialize()
  assert.equal(m.state.status, 'error'); assert.equal(m.state.response, null)
})

test('comparison disposal ignores pending dictionary and query responses', async () => {
  const dict = deferred(); const a = create({ fetchCities: () => dict.promise })
  const first = a.initialize(); a.dispose(); dict.resolve(cities); await first; assert.equal(a.calls.length, 0)
  const pending = deferred(); const b = create({ fetchComparison: () => pending.promise })
  const second = b.initialize(); await Promise.resolve(); await Promise.resolve(); b.dispose()
  pending.resolve(response()); await second; assert.equal(b.state.response, null)
  await b.retry(); assert.equal(b.state.response, null)
})
