import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolveGeoName, validateCityMapping, filterRecordsByTime, joinCityWeatherData, calculateLegendRange, buildWorkbenchCacheKey, createWorkbenchCache, selectDefault } from './weatherWorkbench.js'

// Independent fixture from database/data.sql; never imported into the application.
export const cities = [
  ['JINAN', '济南'], ['QINGDAO', '青岛'], ['ZIBO', '淄博'], ['ZAOZHUANG', '枣庄'],
  ['DONGYING', '东营'], ['YANTAI', '烟台'], ['WEIFANG', '潍坊'], ['JINING', '济宁'],
  ['TAIAN', '泰安'], ['WEIHAI', '威海'], ['RIZHAO', '日照'], ['LINYI', '临沂'],
  ['DEZHOU', '德州'], ['LIAOCHENG', '聊城'], ['BINZHOU', '滨州'], ['HEZE', '菏泽']
].map(([cityCode, cityName], i) => ({ id: i + 101, cityCode, cityName, longitude: 117, latitude: 36 }))
const geo = JSON.parse(readFileSync(new URL('../assets/maps/shandong.json', import.meta.url), 'utf8'))
const records = [
  { cityId: 101, cityName: '济南', forecastTime: '2026-09-07 08:00:00', value: 0 },
  { cityId: 102, cityName: '青岛', forecastTime: '2026-09-07 08:00:00', value: null },
  { cityId: 101, cityName: '济南', forecastTime: '2026-09-07 11:00:00', value: 20.2 }
]

test('matches all 16 real geographic features by code, not dictionary order or name suffix', () => {
  assert.equal(resolveGeoName('JINAN'), '济南市')
  assert.equal(resolveGeoName('UNKNOWN'), null)
  assert.deepEqual(validateCityMapping([...cities].reverse(), geo), { matched: 16, unmatchedCities: [], unmatchedFeatures: [] })
})
test('rejects absent cities and duplicate map features', () => {
  assert.throws(() => validateCityMapping(cities.slice(1), geo), /16/)
  assert.throws(() => validateCityMapping(cities, { ...geo, features: [...geo.features.slice(1), geo.features[1]] }), /16/)
})
test('filters original local business time strings without UTC conversions', () => {
  assert.deepEqual(filterRecordsByTime(records, '2026-09-07 11:00:00'), [records[2]])
})
test('joins via dictionary id; keeps true zero, null and absent measurements distinct', () => {
  const rows = joinCityWeatherData(cities, records, '2026-09-07 08:00:00')
  assert.equal(rows?.length, 16)
  assert.equal(rows[0].name, '济南市')
  assert.equal(rows[0].value, 0)
  assert.equal(rows[0].longitude, 117)
  assert.equal(rows[1].value, null)
  assert.equal(rows[2].value, null)
})
test('temperature legend uses current finite values and expands constant ranges', () => {
  assert.deepEqual(calculateLegendRange([19, 22, null, NaN], 'T2M'), { min: 19, max: 22, empty: false })
  assert.deepEqual(calculateLegendRange([20, 20], 'T2M'), { min: 19, max: 21, empty: false })
  assert.deepEqual(calculateLegendRange([], 'T2M'), { min: 0, max: 1, empty: true })
})
test('precipitation legend starts at zero, including all-zero and negative-only input', () => {
  assert.deepEqual(calculateLegendRange([0, 2.4], 'PRECIP'), { min: 0, max: 2.4, empty: false })
  assert.deepEqual(calculateLegendRange([0, 0.4], 'PRECIP'), { min: 0, max: 0.4, empty: false })
  assert.deepEqual(calculateLegendRange([0], 'PRECIP'), { min: 0, max: 1, empty: false })
  assert.deepEqual(calculateLegendRange([-1, null], 'PRECIP'), { min: 0, max: 1, empty: true })
})
test('cache identity depends on all four query fields, never property insertion order', () => {
  const a = { modelId: 12, elementId: 20, startTime: 'a', endTime: 'b' }
  assert.equal(buildWorkbenchCacheKey(a), '[12,20,"a","b"]')
  assert.equal(buildWorkbenchCacheKey({ ...a, selectedTime: 'x' }), '[12,20,"a","b"]')
  for (const field of Object.keys(a)) assert.notEqual(buildWorkbenchCacheKey(a), buildWorkbenchCacheKey({ ...a, [field]: 'other' }))
})
test('cache stores empty successes and supports explicit invalidation', () => {
  const cache = createWorkbenchCache()
  assert.ok(cache, 'cache must be available')
  cache.set('key', { records: [] })
  assert.deepEqual(cache.get('key'), { records: [] })
  cache.clear()
  assert.equal(cache.get('key'), undefined)
})
test('defaults resolve by code even when IDs/order differ; missing preference uses first item', () => {
  const models = [{ id: 9, modelCode: 'NOAA' }, { id: 42, modelCode: 'ECMWF' }]
  assert.deepEqual(selectDefault(models, 'modelCode', 'ECMWF'), models[1])
  assert.deepEqual(selectDefault(models, 'modelCode', 'MISSING'), models[0])
  assert.equal(selectDefault([], 'modelCode', 'ECMWF'), null)
})
