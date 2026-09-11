import test from 'node:test'
import assert from 'node:assert/strict'

import * as implementation from './trendState.js'
const cities = [{ id: 102, cityCode: 'QINGDAO', cityName: '青岛' }, { id: 101, cityCode: 'JINAN', cityName: '济南' }]
const models = [{ id: 9, modelCode: 'NOAA', modelName: 'NOAA' }, { id: 42, modelCode: 'ECMWF', modelName: 'ECMWF' }]
const range = ['2026-09-01 02:00:00', '2026-09-30 23:00:00']
const payload = params => ({ cityId: params.cityId, cityName: cities.find(c => c.id === params.cityId)?.cityName,
  modelId: params.modelId, modelName: models.find(m => m.id === params.modelId)?.modelName,
  temperature: [{ forecastTime: range[0], value: 19 }], precipitation: [{ forecastTime: range[0], value: 0 }],
  statistics: { temperatureMax: 19, temperatureMin: 19, temperatureAvg: 19, precipitationTotal: 0 } })
const deferred = () => { let resolve, reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no }); return { promise, resolve, reject } }
function setup(fetchTrend = async params => payload(params)) {
  assert.equal(typeof implementation.createTrendState, 'function', 'createTrendState must be implemented')
  const requests = []
  const api = { fetchCities: async () => cities, fetchForecastModels: async () => models,
    fetchTrend: params => { requests.push({ ...params }); return fetchTrend(params) } }
  return { ...implementation.createTrendState(api), api, requests }
}

test('parallel dictionaries select JINAN and ECMWF by code before one initial query', async () => {
  const app = setup(), cityLoad = deferred(), modelLoad = deferred(), started = []
  app.api.fetchCities = () => { started.push('cities'); return cityLoad.promise }
  app.api.fetchForecastModels = () => { started.push('models'); return modelLoad.promise }
  const loading = app.initialize()
  assert.deepEqual(started, ['cities', 'models']); assert.equal(app.state.status, 'loading')
  assert.equal(app.requests.length, 0)
  cityLoad.resolve(cities); modelLoad.resolve(models); await loading
  assert.equal(app.state.cityId, 101); assert.equal(app.state.modelId, 42)
  assert.deepEqual(app.requests, [{ cityId: 101, modelId: 42, startTime: range[0], endTime: range[1] }])
  assert.equal(app.state.status, 'success')
})

test('missing defaults use first supported dictionary option and never fabricate a city/model', async () => {
  const app = setup()
  app.api.fetchCities = async () => [cities[0]]; app.api.fetchForecastModels = async () => [models[0]]
  await app.initialize()
  assert.equal(app.state.cityId, 102); assert.equal(app.state.modelId, 9)
  assert.ok(app.state.warning)
})

test('unsupported temporary dictionaries are excluded and no supported options is retryable error', async () => {
  const app = setup()
  app.api.fetchCities = async () => [{ id: 77, cityCode: 'OTHER', cityName: '临时' }]
  await app.initialize()
  assert.equal(app.state.status, 'error'); assert.equal(app.requests.length, 0)
  app.api.fetchCities = async () => cities; await app.retry()
  assert.equal(app.state.status, 'success')
})

test('editing filters sends no request until explicit query and does not cache repeat queries', async () => {
  const app = setup(); await app.initialize()
  app.state.cityId = 102; app.state.modelId = 9
  await Promise.resolve(); assert.equal(app.requests.length, 1)
  await app.load()
  assert.equal(app.state.response.cityName, '青岛'); assert.equal(app.state.response.modelName, 'NOAA')
  assert.deepEqual(app.state.appliedQuery, { cityId: 102, modelId: 9, startTime: range[0], endTime: range[1] })
  await app.load(); assert.equal(app.requests.length, 3)
})

test('query loading clears previous charts and statistics until the current response arrives', async () => {
  const app = setup(); await app.initialize(); const pending = deferred()
  app.api.fetchTrend = () => pending.promise
  const work = app.load()
  assert.equal(app.state.status, 'loading'); assert.equal(app.state.response, null)
  pending.resolve(payload({ cityId: 101, modelId: 42 })); await work
  assert.equal(app.state.status, 'success')
})

test('empty 200 is empty, preserves metadata and leaves all four statistics null', async () => {
  const app = setup(async params => ({ ...payload(params), temperature: [], precipitation: [],
    statistics: { temperatureMax: null, temperatureMin: null, temperatureAvg: null, precipitationTotal: null } }))
  await app.initialize()
  assert.equal(app.state.status, 'empty'); assert.equal(app.state.response.cityName, '济南')
  assert.deepEqual(Object.values(app.state.response.statistics), [null, null, null, null])
})

test('single available series is success and zero precipitation is not an empty response', async () => {
  const app = setup(async params => ({ ...payload(params), temperature: [],
    statistics: { temperatureMax: null, temperatureMin: null, temperatureAvg: null, precipitationTotal: 0 } }))
  await app.initialize()
  assert.equal(app.state.status, 'success'); assert.equal(app.state.response.precipitation[0].value, 0)
})

test('error hides raw internals and retry uses all current filters without resetting them', async () => {
  let fail = false
  const app = setup(async params => { if (fail) throw new Error('private server details'); return payload(params) })
  await app.initialize(); app.state.cityId = 102; app.state.modelId = 9; app.state.range = ['2026-09-07 11:00:00', range[1]]
  fail = true; await app.load()
  assert.equal(app.state.status, 'error'); assert.equal(app.state.response, null)
  assert.doesNotMatch(app.state.error, /private/)
  fail = false; await app.retry()
  assert.equal(app.state.status, 'success')
  assert.deepEqual(app.requests.at(-1), { cityId: 102, modelId: 9, startTime: '2026-09-07 11:00:00', endTime: range[1] })
})

test('dictionary error ends loading and retry does not reset an edited date range', async () => {
  const app = setup(); app.api.fetchCities = async () => { throw new Error('offline') }
  await app.initialize(); assert.equal(app.state.status, 'error'); assert.equal(app.requests.length, 0)
  app.state.range = [range[0], range[0]]; app.api.fetchCities = async () => cities
  await app.retry(); assert.equal(app.requests[0].endTime, range[0])
})

test('late old success cannot replace newest response even when returning to same model', async () => {
  const app = setup(); await app.initialize(); const old = deferred()
  app.api.fetchTrend = params => params.modelId === 9 ? old.promise : Promise.resolve(payload(params))
  app.state.modelId = 9; const first = app.load()
  app.state.modelId = 42; await app.load()
  old.resolve(payload({ cityId: 101, modelId: 9 })); await first
  assert.equal(app.state.response.modelName, 'ECMWF'); assert.equal(app.state.status, 'success')
})

test('late old failure cannot erase newest successful response', async () => {
  const app = setup(); await app.initialize(); const old = deferred()
  app.api.fetchTrend = params => params.cityId === 102 ? old.promise : Promise.resolve(payload(params))
  app.state.cityId = 102; const first = app.load()
  app.state.cityId = 101; await app.load(); old.reject(new Error('offline')); await first
  assert.equal(app.state.response.cityName, '济南'); assert.equal(app.state.error, '')
})

test('invalid submission invalidates a pending older request without making another API call', async () => {
  const app = setup(); await app.initialize(); const old = deferred()
  app.api.fetchTrend = () => old.promise; const first = app.load()
  app.state.range = [range[1], range[0]]; await app.load()
  old.resolve(payload({ cityId: 101, modelId: 42 })); await first
  assert.equal(app.state.status, 'error'); assert.equal(app.state.response, null)
})

test('wrong response identity is rejected rather than displayed under the current filters', async () => {
  const app = setup(async params => ({ ...payload(params), cityId: 999 }))
  await app.initialize(); assert.equal(app.state.status, 'error'); assert.equal(app.state.response, null)
})

test('unmount invalidates pending dictionaries and trend work and prevents new requests', async () => {
  const pending = deferred(), app = setup(() => pending.promise)
  const work = app.initialize(); await new Promise(resolve => setImmediate(resolve))
  app.dispose(); pending.resolve(payload({ cityId: 101, modelId: 42 })); await work
  assert.equal(app.state.response, null)
  const count = app.requests.length; await app.load(); await app.initialize(); assert.equal(app.requests.length, count)
  const app2 = setup(), dict = deferred(); app2.api.fetchCities = () => dict.promise
  const dictionaryWork = app2.initialize(); app2.dispose(); dict.resolve(cities); await dictionaryWork
  assert.equal(app2.requests.length, 0)
})
