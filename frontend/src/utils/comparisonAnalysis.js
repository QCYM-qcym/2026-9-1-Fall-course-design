import { buildTimeZoom } from './chartZoom.js'
import { validLocalTime, formatTrendTime } from './trendAnalysis.js'
import { ELEMENT_UNITS, WEATHER_ELEMENTS, isDisplayElement, formatElementValue, windDirection } from './weatherElements.js'

export const COMPARISON_MODELS = Object.freeze(['ECMWF', 'NOAA'])
const COLORS = ['#d5dba3', '#70b1cf']
const safeId = id => Number.isSafeInteger(id) && id > 0

export function buildComparisonQuery({ cityId, elementId, range }) {
  if (![cityId, elementId].every(safeId)) throw new Error('请选择有效的城市与气象要素。')
  if (!Array.isArray(range) || range.length !== 2 || !range.every(validLocalTime) || range[0] > range[1]) {
    throw new Error('请选择有效的起止时间，开始时间不能晚于结束时间。')
  }
  return { cityId, elementId, startTime: range[0], endTime: range[1] }
}

export function isComparisonResponse(data) {
  if (!data || !safeId(data.cityId) || typeof data.cityName !== 'string' || !data.element ||
    !safeId(data.element.id) || typeof data.element.elementName !== 'string' ||
    !isDisplayElement(data.element.elementCode) ||
    data.element.unit !== ELEMENT_UNITS[data.element.elementCode] ||
    !Array.isArray(data.series) || data.series.length !== 2) return false
  if (new Set(data.series.map(s => s?.modelId)).size !== 2) return false
  return COMPARISON_MODELS.every(code => {
    const matching = data.series.filter(s => s?.modelCode === code)
    if (matching.length !== 1) return false
    const s = matching[0]
    return safeId(s.modelId) && typeof s.modelName === 'string' && Array.isArray(s.values) &&
      s.values.every(p => p && validLocalTime(p.forecastTime) && Number.isFinite(p.value)) &&
      new Set(s.values.map(p => p.forecastTime)).size === s.values.length
  })
}

export function isComparisonEmpty(data) { return data.series.every(s => s.values.length === 0) }

export function alignComparisonSeries(data) {
  const source = COMPARISON_MODELS.map(code => data.series.find(s => s.modelCode === code))
  const times = [...new Set(source.flatMap(s => s.values.map(p => p.forecastTime)))].sort()
  return { times, series: source.map(s => {
    const byTime = new Map(s.values.map(p => [p.forecastTime, p.value]))
    return { modelCode: s.modelCode, modelName: s.modelName, count: s.values.length,
      data: times.map(time => byTime.has(time) ? byTime.get(time) : null) }
  }) }
}

export function buildComparisonOption(data) {
  const aligned = data ? alignComparisonSeries(data) : { times: [], series: COMPARISON_MODELS.map(modelCode => ({ modelCode, data: [] })) }
  const unit = data?.element.unit ?? ''
  const meta = WEATHER_ELEMENTS[data?.element.elementCode]
  return {
    animation: false, backgroundColor: 'transparent',
    textStyle: { fontFamily: 'Inter, Microsoft YaHei, sans-serif', color: '#afc6d1' },
    legend: { data: [...COMPARISON_MODELS], top: 6, right: 18, textStyle: { color: '#ccdde5' } },
    grid: { top: 54, right: 30, bottom: 80, left: 72 },
    dataZoom: buildTimeZoom(),
    tooltip: { trigger: 'axis', renderMode: 'richText', confine: true, backgroundColor: '#172e3d', borderColor: '#4b6d7a',
      textStyle: { color: '#e6f0f3' }, axisPointer: { type: 'line' },
      formatter: items => {
        const time = (Array.isArray(items) ? items[0] : items)?.axisValue ?? ''
        const index = aligned.times.indexOf(time)
        return [time, ...aligned.series.map(s => `${s.modelCode}  ${formatElementValue(s.data[index], meta?.code)}`)].join('\n')
      } },
    xAxis: { type: 'category', boundaryGap: false, data: aligned.times,
      axisLabel: { formatter: formatTrendTime, color: '#9eb8c6', hideOverlap: true, margin: 12 },
      axisLine: { lineStyle: { color: '#3c5667' } }, axisTick: { show: false } },
    yAxis: { type: 'value', name: unit, scale: meta?.min === undefined,
      ...(meta?.min !== undefined ? { min: meta.min } : {}),
      ...(meta?.max !== undefined ? { max: meta.max } : {}),
      ...(meta?.displayType === 'direction' ? { interval: 90 } : {}),
      axisLabel: { color: '#9eb8c6', ...(meta?.displayType === 'direction' ? { formatter: value => `${value}° ${windDirection(value)}` } : {}) }, splitLine: { lineStyle: { color: '#2a4556', type: 'dashed' } } },
    series: aligned.series.map((s, i) => ({ id: `comparison-${s.modelCode}`, name: s.modelCode, type: 'line', data: s.data,
      smooth: false, connectNulls: false, showSymbol: aligned.times.length <= 32, symbol: i === 0 ? 'circle' : 'diamond', symbolSize: 7,
      itemStyle: { color: COLORS[i] }, lineStyle: { color: COLORS[i], width: 3, type: i === 0 ? 'solid' : 'dashed' } }))
  }
}
