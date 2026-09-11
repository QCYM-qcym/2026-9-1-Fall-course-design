import test from 'node:test'
import assert from 'node:assert/strict'
import * as comparison from './comparisonAnalysis.js'
import * as weather from '../api/weather.js'
import http from '../api/http.js'
import { init, use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { SVGRenderer } from 'echarts/renderers'

const required = name => { assert.equal(typeof comparison[name], 'function', `${name} must be implemented`); return comparison[name] }
const times = ['2026-09-07 08:00:00', '2026-09-07 11:00:00', '2026-09-07 14:00:00']
const points = values => values.map((value, i) => ({ forecastTime: times[i], value }))
const payload = () => ({ cityId: 8, cityName: '济南', element: { id: 71, elementCode: 'T2M', elementName: '2 米气温', unit: '℃' },
  series: [{ modelId: 41, modelCode: 'ECMWF', modelName: 'ECMWF', values: points([19, 20.2, 21.1]) },
    { modelId: 59, modelCode: 'NOAA', modelName: 'NOAA', values: points([18.4, 19.6, 20.5]) }] })

for(const [elementCode,unit] of [['TCC','%'],['WIND_SPEED_100M','m/s'],['WIND_DIR_100M','°'],['RH','%']]) test(`comparison accepts ${elementCode} and preserves its unit in axis and tooltip`,()=>{
  const p=payload(); p.element={id:100,elementCode,elementName:elementCode,unit}
  assert.equal(comparison.isComparisonResponse(p),true)
  const option=comparison.buildComparisonOption(p)
  assert.equal(option.yAxis.name,unit); assert.ok(option.tooltip.formatter([{axisValue:times[0]}]).includes(` ${unit}`))
  assert.ok(option.series.every(s=>s.type==='line'))
  p.element.unit='mm'; assert.equal(comparison.isComparisonResponse(p),false)
})

test('comparison client sends GET and exactly four parameters via existing Axios', async () => {
  assert.equal(typeof weather.fetchComparison, 'function')
  const previous = http.defaults.adapter
  let request
  http.defaults.adapter = async config => { request = config; return { data: { code: 200, data: payload() }, status: 200, headers: {}, config } }
  try {
    assert.deepEqual(await weather.fetchComparison({ cityId: 8, elementId: 71, startTime: times[0], endTime: times[2], modelId: 41 }), payload())
    assert.equal(request.method, 'get'); assert.equal(request.baseURL, '/api'); assert.equal(request.url, '/weather/comparison')
    assert.deepEqual(request.params, { cityId: 8, elementId: 71, startTime: times[0], endTime: times[2] })
  } finally { http.defaults.adapter = previous }
})

test('comparison client rejects failed and invalid responses', async () => {
  assert.equal(typeof weather.fetchComparison, 'function')
  const previous = http.defaults.adapter
  try {
    for (const body of [{ code: 500, data: null }, { code: 200, data: { ...payload(), series: [] } }, { code: 200, data: null }]) {
      http.defaults.adapter = async config => ({ data: body, status: 200, headers: {}, config })
      await assert.rejects(weather.fetchComparison({ cityId: 8, elementId: 71, startTime: times[0], endTime: times[2] }))
    }
  } finally { http.defaults.adapter = previous }
})

test('comparison query preserves local time including equal endpoints', () => {
  const build = required('buildComparisonQuery')
  assert.deepEqual(build({ cityId: 8, elementId: 71, range: [times[0], times[2]] }),
    { cityId: 8, elementId: 71, startTime: times[0], endTime: times[2] })
  assert.equal(build({ cityId: 8, elementId: 71, range: [times[0], times[0]] }).endTime, times[0])
})

test('comparison rejects unsafe IDs and invalid local dates without UTC conversion', () => {
  const build = required('buildComparisonQuery')
  for (const id of [0, -1, null, 1.5, Number.MAX_SAFE_INTEGER + 1]) {
    assert.throws(() => build({ cityId: id, elementId: 71, range: times.slice(0, 2) }))
    assert.throws(() => build({ cityId: 8, elementId: id, range: times.slice(0, 2) }))
  }
  for (const range of [null, [], [times[2], times[0]], ['2026-02-30 08:00:00', times[2]],
    ['2026-09-07T08:00:00Z', times[2]], ['2026-09-07 08:00:00+08:00', times[2]]]) {
    assert.throws(() => build({ cityId: 8, elementId: 71, range }))
  }
})

test('comparison response validates two model identities, units, real values and unique times', () => {
  const valid = required('isComparisonResponse')
  assert.equal(valid(payload()), true)
  const empty = payload(); empty.series.forEach(s => { s.values = [] }); assert.equal(valid(empty), true)
  for (const mutate of [p => { p.series.pop() }, p => { p.series[1].modelCode = 'ECMWF' },
    p => { p.series[1].modelId = 41 }, p => { p.element.unit = 'K' }, p => { p.series[0].values[0].value = null },
    p => { p.series[0].values.push(p.series[0].values[0]) }, p => { p.series[0].values[0].forecastTime = 'bad' }]) {
    const p = payload(); mutate(p); assert.equal(valid(p), false)
  }
})

test('comparison aligns matching model times and keeps fixed model identity despite array order', () => {
  const p = payload(); p.series.reverse(); p.series.forEach(s => s.values.reverse())
  const aligned = required('alignComparisonSeries')(p)
  assert.deepEqual(aligned.times, times)
  assert.deepEqual(aligned.series.map(s => s.modelCode), ['ECMWF', 'NOAA'])
  assert.deepEqual(aligned.series.map(s => s.data), [[19, 20.2, 21.1], [18.4, 19.6, 20.5]])
  assert.equal(p.series[0].modelCode, 'NOAA', 'does not mutate response')
})

for (const missing of ['ECMWF', 'NOAA']) {
  test(`comparison ${missing} missing middle point becomes null not zero`, () => {
    const p = payload(); p.series.find(s => s.modelCode === missing).values.splice(1, 1)
    const aligned = required('alignComparisonSeries')(p)
    assert.deepEqual(aligned.times, times)
    assert.equal(aligned.series.find(s => s.modelCode === missing).data[1], null)
    assert.equal(aligned.series.find(s => s.modelCode !== missing).data[1], missing === 'ECMWF' ? 19.6 : 20.2)
  })
}

test('comparison single empty model keeps identity and null aligned values; both empty stay empty', () => {
  const p = payload(); p.series[0].values = []
  assert.equal(required('isComparisonEmpty')(p), false)
  assert.deepEqual(required('alignComparisonSeries')(p).series[0].data, [null, null, null])
  p.series[1].values = []
  assert.equal(required('isComparisonEmpty')(p), true)
  assert.deepEqual(required('alignComparisonSeries')(p).times, [])
  assert.deepEqual(required('alignComparisonSeries')(p).series.map(s => s.data), [[], []])
})

test('comparison T2M option is two unsmoothed lines, fixed legend and local axis', () => {
  const option = required('buildComparisonOption')(payload())
  assert.deepEqual(option.series.map(s => s.type), ['line', 'line'])
  assert.deepEqual(option.legend.data, ['ECMWF', 'NOAA'])
  assert.deepEqual(option.xAxis.data, times); assert.equal(option.yAxis.name, '℃')
  assert.equal(option.xAxis.axisLabel.formatter(times[0]), '09-07 08:00')
  assert.ok(option.series.every(s => s.connectNulls === false && s.smooth === false))
  assert.notEqual(option.series[0].itemStyle.color, option.series[1].itemStyle.color)
})

test('comparison PRECIP remains two lines and real zero is visible with mm tooltip', () => {
  const p = payload(); p.element = { id: 83, elementCode: 'PRECIP', elementName: '降水量', unit: 'mm' }
  p.series[0].values = points([0, 0.4, 0.2]); p.series[1].values = points([0.1, 0.6, 0.3])
  const option = required('buildComparisonOption')(p)
  assert.deepEqual(option.series.map(s => s.type), ['line', 'line']); assert.equal(option.yAxis.name, 'mm')
  assert.equal(option.series[0].data[0], 0)
  assert.match(option.tooltip.formatter([{ axisValue: times[0] }]), /ECMWF.*0\.00 mm[\s\S]*NOAA.*0\.10 mm/)
})

test('comparison tooltip shows both models and missing marker even if ECharts omits null item', () => {
  const p = payload(); p.series[0].values.splice(1, 1)
  const option = required('buildComparisonOption')(p)
  const text = option.tooltip.formatter([{ axisValue: times[1], seriesName: 'NOAA', value: 19.6 }])
  assert.match(text, /2026-09-07 11:00:00/); assert.match(text, /ECMWF.*--/); assert.match(text, /NOAA.*19\.60 ℃/)
})

test('comparison real ECharts renders both lines, changes unit, clears empty, resizes and disposes', () => {
  const build = required('buildComparisonOption')
  use([LineChart, GridComponent, TooltipComponent, LegendComponent, SVGRenderer])
  const warnings = [], previous = { log: console.log, warn: console.warn }
  console.log = console.warn = (...args) => warnings.push(args.join(' '))
  const chart = init(null, null, { renderer: 'svg', ssr: true, width: 1000, height: 380 })
  try {
    const p = payload(); chart.setOption(build(p), { notMerge: true })
    assert.match(chart.renderToSVGString(), /ECMWF/); assert.match(chart.renderToSVGString(), /NOAA/)
    p.element = { id: 83, elementCode: 'PRECIP', elementName: '降水量', unit: 'mm' }
    chart.setOption(build(p), { notMerge: true }); assert.equal(chart.getOption().yAxis[0].name, 'mm')
    p.series.forEach(s => { s.values = [] }); chart.setOption(build(p), { notMerge: true })
    assert.deepEqual(chart.getOption().series.map(s => s.data), [[], []])
    chart.resize({ width: 800, height: 320 }); assert.equal(chart.getWidth(), 800)
  } finally { chart.dispose(); console.log = previous.log; console.warn = previous.warn }
  assert.equal(chart.isDisposed(), true); assert.deepEqual(warnings, [])
})
