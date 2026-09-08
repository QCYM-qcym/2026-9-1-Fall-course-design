import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { init, registerMap, use } from 'echarts/core'
import { MapChart } from 'echarts/charts'
import { VisualMapContinuousComponent, TooltipComponent } from 'echarts/components'
import { SVGRenderer } from 'echarts/renderers'
import { createMapOption } from './mapOption.js'

use([MapChart, VisualMapContinuousComponent, TooltipComponent, SVGRenderer])
const geo = JSON.parse(readFileSync(new URL('../assets/maps/shandong.json', import.meta.url), 'utf8'))
registerMap('shandong-course', geo)
const rows = geo.features.map((feature, index) => ({ id: index + 1, name: feature.properties.name, cityName: feature.properties.name, value: index === 0 ? null : index }))
test('real ECharts registers and renders all 16 regions, missing data stays missing, and disposes', () => {
  const chart = init(null, null, { renderer: 'svg', ssr: true, width: 1000, height: 600 })
  try {
    chart.setOption(createMapOption(rows, { min: 1, max: 15 }, 'T2M', '℃', null))
    const svg = chart.renderToSVGString()
    assert.match(svg, /济南/)
    assert.match(svg, /菏泽/)
    assert.match(svg, /暂无数据/)
    assert.equal(chart.getOption().series[0].data.length, 16)
    chart.setOption(createMapOption(rows, { min: 0, max: 15 }, 'PRECIP', 'mm', 2))
    assert.equal(chart.getOption().visualMap[0].min, 0)
  } finally { chart.dispose() }
  assert.equal(chart.isDisposed(), true)
})
