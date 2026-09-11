import test from 'node:test'
import assert from 'node:assert/strict'
import { WEATHER_ELEMENTS, formatElementValue, windDirection } from './weatherElements.js'
import { groupForecastTimes, selectDateTime, MONTHLY_RANGE, calculateLegendRange } from './weatherWorkbench.js'
import { buildTemperatureOption, buildPrecipitationOption } from './trendAnalysis.js'
import { buildComparisonOption } from './comparisonAnalysis.js'
import { init, use } from 'echarts/core'
import { LineChart, BarChart } from 'echarts/charts'
import { DataZoomComponent, GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { SVGRenderer } from 'echarts/renderers'

const times = Array.from({ length: 30 }, (_, day) =>
  [2, 5, 8, 11, 14, 17, 20, 23].map(hour => `2026-09-${String(day + 1).padStart(2, '0')} ${String(hour).padStart(2, '0')}:00:00`)).flat()

test('monthly date choices contain only the actual returned eight slots per day', () => {
  const days = groupForecastTimes([...times].reverse())
  assert.equal(days.length, 30)
  assert.deepEqual(days[6], { date: '2026-09-07', times: times.slice(48, 56) })
  assert.deepEqual(MONTHLY_RANGE, [times[0], times.at(-1)])
  assert.deepEqual(groupForecastTimes([]), [])
})

test('date switch preserves the clock slot, falls back to first real slot, and ignores absent dates', () => {
  assert.equal(selectDateTime(times, '2026-09-08', '2026-09-07 14:00:00'), '2026-09-08 14:00:00')
  const sparse = ['2026-09-09 05:00:00', '2026-09-09 20:00:00']
  assert.equal(selectDateTime(sparse, '2026-09-09', '2026-09-08 14:00:00'), sparse[0])
  assert.equal(selectDateTime(sparse, '2026-09-10', sparse[0]), sparse[0])
})

test('six elements share display metadata, precision and units without converting missing values to zero', () => {
  assert.deepEqual(Object.keys(WEATHER_ELEMENTS).sort(), ['PRECIP', 'RH', 'T2M', 'TCC', 'WIND_DIR_100M', 'WIND_SPEED_100M'])
  for (const [code, meta] of Object.entries(WEATHER_ELEMENTS)) {
    assert.equal(meta.code, code)
    assert.ok(meta.name && meta.unit && meta.legendTitle && meta.displayType)
    assert.ok(Number.isInteger(meta.precision))
    assert.equal(formatElementValue(null, code), '--')
    assert.equal(formatElementValue(undefined, code), '--')
    assert.notEqual(formatElementValue(0, code), '--')
  }
  assert.equal(formatElementValue(19, 'T2M'), '19.00 ℃')
  assert.equal(formatElementValue(0, 'PRECIP'), '0.00 mm')
  assert.equal(formatElementValue(237.5, 'WIND_DIR_100M'), '237.5 ° · 西南风')
})

test('wind direction has eight Chinese bearings and correctly wraps north at 360', () => {
  assert.deepEqual([0, 45, 90, 135, 180, 225, 270, 315, 360].map(windDirection), ['北', '东北', '东', '东南', '南', '西南', '西', '西北', '北'])
  assert.equal(windDirection(22.49), '北')
  assert.equal(windDirection(22.5), '东北')
  assert.equal(windDirection(null), '')
})

test('percentage and direction legends retain physical meaning; wind speed starts at zero', () => {
  for (const code of ['TCC', 'RH']) assert.deepEqual(calculateLegendRange([35, 70], code), { min: 0, max: 100, empty: false })
  assert.deepEqual(calculateLegendRange([237.5], 'WIND_DIR_100M'), { min: 0, max: 360, empty: false })
  assert.deepEqual(calculateLegendRange([3, 8], 'WIND_SPEED_100M'), { min: 0, max: 8, empty: false })
})

test('240 point trend charts provide slider and inside zoom with readable axes and tooltips', () => {
  const points = times.map(forecastTime => ({ forecastTime, value: 0 }))
  for (const build of [buildTemperatureOption, buildPrecipitationOption]) {
    const option = build(points)
    assert.deepEqual(option.dataZoom.map(z => z.type), ['slider', 'inside'])
    assert.notEqual(option.xAxis.axisLabel.interval, 0)
    assert.ok(option.grid.bottom >= 75)
    assert.match(option.tooltip.formatter([{ axisValue: times[0], value: 0 }]), /2026-09-01 02:00:00\n.*0\.00 (℃|mm)/)
  }
  assert.equal(buildPrecipitationOption(points).series[0].label.show, false)
})

test('comparison zoom preserves gaps, zero, both models and direction semantics', () => {
  const data = { element: { elementCode: 'WIND_DIR_100M', unit: '°' }, series: [
    { modelCode: 'ECMWF', values: [{ forecastTime: times[0], value: 237.5 }, { forecastTime: times[1], value: 0 }] },
    { modelCode: 'NOAA', values: [{ forecastTime: times[0], value: 90 }] }
  ] }
  const option = buildComparisonOption(data)
  assert.deepEqual(option.dataZoom.map(z => z.type), ['slider', 'inside'])
  assert.deepEqual(option.series[1].data, [90, null])
  assert.match(option.tooltip.formatter([{ axisValue: times[0] }]), /ECMWF.*西南风\nNOAA.*东风/)
  assert.match(option.tooltip.formatter([{ axisValue: times[1] }]), /ECMWF.*0\.0 ° · 北风\nNOAA.*--/)
  assert.equal(option.yAxis.min, 0)
  assert.equal(option.yAxis.max, 360)
})

test('real ECharts can zoom and reset all 240-point charts without losing zero or null gaps', () => {
  use([LineChart, BarChart, DataZoomComponent, GridComponent, TooltipComponent, LegendComponent, SVGRenderer])
  const points = times.map(forecastTime => ({ forecastTime, value: 0 }))
  const comparison = { element: { elementCode: 'PRECIP', unit: 'mm' }, series: [
    { modelCode: 'ECMWF', values: points },
    { modelCode: 'NOAA', values: points.filter((_, i) => i !== 120) }
  ] }
  for (const option of [buildTemperatureOption(points), buildPrecipitationOption(points), buildComparisonOption(comparison)]) {
    const chart = init(null, null, { renderer: 'svg', ssr: true, width: 1100, height: 340 })
    try {
      chart.setOption(option)
      chart.dispatchAction({ type: 'dataZoom', start: 45, end: 55 })
      assert.equal(chart.getOption().dataZoom[0].start, 45)
      assert.equal(chart.getOption().dataZoom[1].end, 55)
      assert.ok(chart.renderToSVGString().includes('<svg'))
      assert.equal(chart.getOption().series[0].data[120], 0)
      if (option.series.length === 2) assert.equal(chart.getOption().series[1].data[120], null)
      chart.dispatchAction({ type: 'dataZoom', start: 0, end: 100 })
      assert.equal(chart.getOption().dataZoom[0].end, 100)
    } finally { chart.dispose() }
  }
})
