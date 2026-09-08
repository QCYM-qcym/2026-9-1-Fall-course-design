import test from 'node:test'
import assert from 'node:assert/strict'
import { init, use } from 'echarts/core'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { SVGRenderer } from 'echarts/renderers'
import http from '../api/http.js'
import * as weather from '../api/weather.js'
import * as trend from './trendAnalysis.js'
const required = name => { assert.equal(typeof trend[name], 'function', `${name} must be implemented`); return trend[name] }
const times = ['2026-09-07 08:00:00', '2026-09-07 11:00:00', '2026-09-07 14:00:00']
const temperature = times.map((forecastTime, i) => ({ forecastTime, value: [19, 20.2, 21.1][i] }))
const precipitation = times.map((forecastTime, i) => ({ forecastTime, value: [0, 0.4, 0.2][i] }))
const payload = { cityId: 101, cityName: '济南', modelId: 42, modelName: 'ECMWF', temperature, precipitation,
  statistics: { temperatureMax: 21.1, temperatureMin: 19, temperatureAvg: 20.1, precipitationTotal: 0.6 } }

test('fetchTrend sends only the four contract parameters through the existing /api client', async () => {
  assert.equal(typeof weather.fetchTrend, 'function', 'fetchTrend must be implemented')
  const previous = http.defaults.adapter
  let request
  http.defaults.adapter = async config => { request = config; return { data: { code: 200, message: 'success', data: payload }, status: 200, statusText: 'OK', headers: {}, config } }
  try {
    const data = await weather.fetchTrend({ cityId: 101, modelId: 42, startTime: times[0], endTime: times[2], elementId: 7 })
    assert.equal(request.baseURL, '/api'); assert.equal(request.url, '/weather/trend')
    assert.deepEqual(request.params, { cityId: 101, modelId: 42, startTime: times[0], endTime: times[2] })
    assert.deepEqual(data, payload)
  } finally { http.defaults.adapter = previous }
})

test('fetchTrend rejects business failure and malformed series instead of treating either as empty', async () => {
  assert.equal(typeof weather.fetchTrend, 'function')
  const previous = http.defaults.adapter
  try {
    for (const body of [{ code: 500, data: null }, { code: 200, data: { ...payload, temperature: null } },
      { code: 200, data: { ...payload, statistics: {} } }]) {
      http.defaults.adapter = async config => ({ data: body, status: 200, statusText: 'OK', headers: {}, config })
      await assert.rejects(weather.fetchTrend({ cityId: 101, modelId: 42, startTime: times[0], endTime: times[2] }))
    }
  } finally { http.defaults.adapter = previous }
})

test('buildTrendQuery preserves local business time and accepts equal endpoints', () => {
  const build = required('buildTrendQuery')
  assert.deepEqual(build({ cityId: 101, modelId: 42, range: [times[0], times[2]] }),
    { cityId: 101, modelId: 42, startTime: times[0], endTime: times[2] })
  assert.equal(build({ cityId: 101, modelId: 42, range: [times[0], times[0]] }).endTime, times[0])
})

test('query rejects reversed, impossible, UTC, cleared dates and unsafe IDs without silently converting', () => {
  const build = required('buildTrendQuery')
  for (const range of [null, [], [times[2], times[0]], ['2026-02-30 08:00:00', times[2]],
    ['2026-09-07T08:00:00Z', times[2]], ['2026-09-07 24:00:00', times[2]]]) {
    assert.throws(() => build({ cityId: 101, modelId: 42, range }))
  }
  for (const id of [0, -1, null, 1.5, Number.MAX_SAFE_INTEGER + 1]) {
    assert.throws(() => build({ cityId: id, modelId: 42, range: times.slice(0, 2) }))
    assert.throws(() => build({ cityId: 101, modelId: id, range: times.slice(0, 2) }))
  }
})

test('metrics preserve true zero and display null or undefined as --', () => {
  const format = required('formatMetricValue')
  assert.equal(format(0), '0.00'); assert.equal(format(21.1), '21.10')
  assert.equal(format(null), '--'); assert.equal(format(undefined), '--'); assert.equal(format(NaN), '--')
})

test('statistics cards map the four exact backend fields without deriving values from series', () => {
  const cards = required('buildTrendStatistics')({ temperatureMax: 99, temperatureMin: -8, temperatureAvg: 12.34, precipitationTotal: 0 })
  assert.deepEqual(cards.map(card => [card.key, card.value, card.unit]), [
    ['temperatureMax', '99.00', '℃'], ['temperatureMin', '-8.00', '℃'],
    ['temperatureAvg', '12.34', '℃'], ['precipitationTotal', '0.00', 'mm']])
  assert.deepEqual(required('buildTrendStatistics')(null).map(card => card.value), ['--', '--', '--', '--'])
})

test('chart time labels are string slices with no UTC offset', () => {
  assert.equal(required('formatTrendTime')(times[0]), '09-07 08:00')
})

test('temperature option has three line points, local times and a unit-bearing tooltip', () => {
  const option = required('buildTemperatureOption')(temperature)
  assert.equal(option.series[0].type, 'line'); assert.equal(option.yAxis.name, '℃')
  assert.deepEqual(option.xAxis.data, times); assert.deepEqual(option.series[0].data, [19, 20.2, 21.1])
  assert.match(option.tooltip.formatter([{ axisValue: times[0], value: 19 }]), /2026-09-07 08:00:00[\s\S]*19\.00 ℃/)
})

test('precipitation option is a bar series retaining zero with mm tooltip', () => {
  const option = required('buildPrecipitationOption')(precipitation)
  assert.equal(option.series[0].type, 'bar'); assert.equal(option.yAxis.name, 'mm')
  assert.deepEqual(option.series[0].data, [0, 0.4, 0.2])
  assert.match(option.tooltip.formatter([{ axisValue: times[0], value: 0 }]), /0\.00 mm/)
})

test('mismatched series retain their own timestamps and empty options contain no fabricated points', () => {
  const temp = required('buildTemperatureOption')([temperature[0], temperature[2]])
  const rain = required('buildPrecipitationOption')([precipitation[1], precipitation[2]])
  assert.deepEqual(temp.xAxis.data, [times[0], times[2]])
  assert.deepEqual(rain.xAxis.data, [times[1], times[2]])
  assert.deepEqual(temp.series[0].data, [19, 21.1]); assert.deepEqual(rain.series[0].data, [0.4, 0.2])
  assert.deepEqual(required('buildTemperatureOption')([]).series[0].data, [])
})

test('real ECharts renders both trend options, replaces data, resizes and disposes', () => {
  const buildTemp = required('buildTemperatureOption'), buildRain = required('buildPrecipitationOption')
  use([LineChart, BarChart, GridComponent, TooltipComponent, SVGRenderer])
  const warnings = []
  const previousWarn = console.warn
  const previousLog = console.log
  console.warn = (...args) => warnings.push(args.join(' '))
  console.log = (...args) => warnings.push(args.join(' '))
  try {
    for (const [builder, points, type] of [[buildTemp, temperature, 'line'], [buildRain, precipitation, 'bar']]) {
      const chart = init(null, null, { renderer: 'svg', ssr: true, width: 1000, height: 260 })
      try {
        chart.setOption(builder(points), { notMerge: true })
        assert.equal(chart.getOption().series[0].type, type)
        assert.match(chart.renderToSVGString(), /09-07 08:00/)
        chart.resize({ width: 800, height: 240 }); assert.equal(chart.getWidth(), 800)
        chart.setOption(builder([]), { notMerge: true }); assert.deepEqual(chart.getOption().series[0].data, [])
      } finally { chart.dispose() }
      assert.equal(chart.isDisposed(), true)
    }
  } finally { console.warn = previousWarn; console.log = previousLog }
  assert.deepEqual(warnings, [], 'trend options must not generate ECharts warnings')
})
