import { buildTimeZoom } from './chartZoom.js'
import { formatElementValue } from './weatherElements.js'

const STATISTICS = Object.freeze([
  ['temperatureMax', '最高温度', 'MAX TEMPERATURE', '℃'],
  ['temperatureMin', '最低温度', 'MIN TEMPERATURE', '℃'],
  ['temperatureAvg', '平均温度', 'AVG TEMPERATURE', '℃'],
  ['precipitationTotal', '累计降水', 'TOTAL PRECIPITATION', 'mm']
])

export function validLocalTime(value) {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(value)) return false
  const [year, month, day, hour, minute, second] = value.split(/[- :]/).map(Number)
  const leap = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0)
  const days = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
  return year >= 1 && month >= 1 && month <= 12 && day >= 1 && day <= days[month - 1] && hour < 24 && minute < 60 && second < 60
}

export function buildTrendQuery({ cityId, modelId, range }) {
  if (![cityId, modelId].every(id => Number.isSafeInteger(id) && id > 0)) throw new Error('请选择有效的城市与模型。')
  if (!Array.isArray(range) || range.length !== 2 || !range.every(validLocalTime) || range[0] > range[1]) {
    throw new Error('请选择有效的起止时间，开始时间不能晚于结束时间。')
  }
  return { cityId, modelId, startTime: range[0], endTime: range[1] }
}

export function isTrendResponse(data) {
  const seriesValid = series => Array.isArray(series) && series.every(point =>
    point && validLocalTime(point.forecastTime) && Number.isFinite(point.value))
  return data && Number.isSafeInteger(data.cityId) && data.cityId > 0 && typeof data.cityName === 'string' &&
    Number.isSafeInteger(data.modelId) && data.modelId > 0 && typeof data.modelName === 'string' &&
    seriesValid(data.temperature) && seriesValid(data.precipitation) && data.statistics &&
    STATISTICS.every(([key]) => Object.hasOwn(data.statistics, key) &&
      (data.statistics[key] === null || Number.isFinite(data.statistics[key])))
}

export function formatMetricValue(value) {
  return Number.isFinite(value) ? value.toFixed(2) : '--'
}

export function buildTrendStatistics(statistics) {
  return STATISTICS.map(([key, label, caption, unit]) => ({ key, label, caption, unit, value: formatMetricValue(statistics?.[key]) }))
}

export function formatTrendTime(time) {
  return typeof time === 'string' ? time.slice(5, 16) : ''
}

function buildOption(points, type, unit, color, name) {
  return {
    animation: false,
    backgroundColor: 'transparent',
    textStyle: { fontFamily: 'Inter, Microsoft YaHei, sans-serif', color: '#a9c2cf' },
    grid: { top: 28, right: 30, bottom: 78, left: 62 },
    dataZoom: buildTimeZoom(),
    tooltip: {
      trigger: 'axis', renderMode: 'richText', backgroundColor: '#172e3d', borderColor: '#4b6d7a',
      textStyle: { color: '#e6f0f3' }, confine: true,
      formatter: items => {
        const point = Array.isArray(items) ? items[0] : items
        return `${point?.axisValue ?? ''}\n${name}  ${formatElementValue(point?.value, type === 'line' ? 'T2M' : 'PRECIP')}`
      }
    },
    xAxis: { type: 'category', boundaryGap: type === 'bar', data: points.map(point => point.forecastTime),
      axisLabel: { color: '#9eb8c6', formatter: formatTrendTime, hideOverlap: true, margin: 12 },
      axisLine: { lineStyle: { color: '#3c5667' } }, axisTick: { show: false } },
    yAxis: { type: 'value', name: unit, nameTextStyle: { color: '#a9c2cf', padding: [0, 0, 0, 6] },
      scale: type === 'line', ...(type === 'bar' ? { min: 0 } : {}),
      axisLabel: { color: '#9eb8c6' }, splitLine: { lineStyle: { color: '#2a4556', type: 'dashed' } } },
    series: [{ id: type === 'line' ? 'trend-temperature' : 'trend-precipitation', name, type,
      data: points.map(point => point.value), itemStyle: { color },
      ...(type === 'line' ? { showSymbol: points.length <= 32, symbolSize: 7, connectNulls: false, smooth: false,
        lineStyle: { width: 3, color }, areaStyle: { color, opacity: 0.08 } }
        : { barMaxWidth: 48, itemStyle: { color, borderRadius: [5, 5, 0, 0] },
          label: { show: points.length <= 16, position: 'top', color: '#b6d8e4', formatter: params => formatMetricValue(params.value) } })
    }]
  }
}

export function buildTemperatureOption(points) { return buildOption(points, 'line', '℃', '#d5dba3', '2 米气温') }
export function buildPrecipitationOption(points) { return buildOption(points, 'bar', 'mm', '#70b1cf', '降水量') }
