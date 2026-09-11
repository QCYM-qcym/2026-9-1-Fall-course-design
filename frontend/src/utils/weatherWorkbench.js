import { WEATHER_ELEMENTS } from './weatherElements.js'
// Codes are the database dictionary contract; feature names are the map adapter.
const GEO_NAMES = Object.freeze({
  JINAN: '济南市', QINGDAO: '青岛市', ZIBO: '淄博市', ZAOZHUANG: '枣庄市',
  DONGYING: '东营市', YANTAI: '烟台市', WEIFANG: '潍坊市', JINING: '济宁市',
  TAIAN: '泰安市', WEIHAI: '威海市', RIZHAO: '日照市', LINYI: '临沂市',
  DEZHOU: '德州市', LIAOCHENG: '聊城市', BINZHOU: '滨州市', HEZE: '菏泽市'
})

export const DEMO_RANGE = Object.freeze(['2026-09-07 08:00:00', '2026-09-07 14:00:00'])
export const MONTHLY_RANGE = Object.freeze(['2026-09-01 02:00:00', '2026-09-30 23:00:00'])
export const TEMPERATURE_COLORS = WEATHER_ELEMENTS.T2M.colors
export const PRECIPITATION_COLORS = WEATHER_ELEMENTS.PRECIP.colors

export function groupForecastTimes(times) {
  const days = new Map()
  for (const time of [...new Set(times)].sort()) {
    const date = time.slice(0, 10)
    if (!days.has(date)) days.set(date, [])
    days.get(date).push(time)
  }
  return [...days].map(([date, times]) => ({ date, times }))
}

export function selectDateTime(times, date, selectedTime) {
  const slots = groupForecastTimes(times).find(day => day.date === date)?.times ?? []
  return slots.find(time => time.slice(11) === selectedTime?.slice(11)) ?? slots[0] ?? selectedTime
}

export function resolveGeoName(code) {
  return Object.hasOwn(GEO_NAMES, code) ? GEO_NAMES[code] : null
}

export function validateCityMapping(cities, geo) {
  const features = geo?.features ?? []
  const names = features.map(feature => feature.properties?.name)
  const codes = cities.map(city => city.cityCode)
  const unmatchedCities = cities.filter(city => !names.includes(resolveGeoName(city.cityCode)))
  const unmatchedFeatures = names.filter(name => !cities.some(city => resolveGeoName(city.cityCode) === name))
  if (cities.length !== 16 || new Set(codes).size !== 16 || new Set(cities.map(c => c.id)).size !== 16 ||
      features.length !== 16 || new Set(names).size !== 16 || unmatchedCities.length || unmatchedFeatures.length ||
      features.some(feature => !['Polygon', 'MultiPolygon'].includes(feature.geometry?.type))) {
    throw new Error('山东地图与城市字典必须完整匹配 16 市，请检查数据来源及城市编码。')
  }
  return { matched: 16, unmatchedCities, unmatchedFeatures }
}

export function filterRecordsByTime(records, time) {
  return records.filter(record => record.forecastTime === time)
}

export function joinCityWeatherData(cities, records, time) {
  const byId = new Map(filterRecordsByTime(records, time).map(record => [record.cityId, record]))
  return cities.map(city => ({
    ...city,
    name: resolveGeoName(city.cityCode),
    value: Number.isFinite(byId.get(city.id)?.value) ? byId.get(city.id).value : null
  }))
}

export function calculateLegendRange(values, elementCode) {
  const meta = WEATHER_ELEMENTS[elementCode]
  const finite = values.filter(value => Number.isFinite(value) && (meta?.min === undefined || value >= meta.min))
  if (meta?.max !== undefined) return { min: meta.min, max: meta.max, empty: !finite.length }
  if (!finite.length) return { min: 0, max: 1, empty: true }
  if (meta?.min === 0) return { min: 0, max: Math.max(...finite) || 1, empty: false }
  const min = Math.min(...finite)
  const max = Math.max(...finite)
  return { min: min === max ? min - 1 : min, max: min === max ? max + 1 : max, empty: false }
}

export function buildWorkbenchCacheKey({ modelId, elementId, startTime, endTime }) {
  return JSON.stringify([modelId, elementId, startTime, endTime])
}

export function createWorkbenchCache() {
  return new Map()
}

export function selectDefault(items, field, code) {
  return items.find(item => item[field] === code) ?? items[0] ?? null
}
