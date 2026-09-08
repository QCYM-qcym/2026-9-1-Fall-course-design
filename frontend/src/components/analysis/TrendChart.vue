<script setup>
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { init, use } from 'echarts/core'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { buildTemperatureOption, buildPrecipitationOption } from '../../utils/trendAnalysis.js'

use([LineChart, BarChart, GridComponent, TooltipComponent, CanvasRenderer])
const props = defineProps({ kind: { type: String, required: true }, points: { type: Array, default: () => [] }, status: String })
const container = ref(null)
const chartFailed = ref(false)
const isTemperature = computed(() => props.kind === 'temperature')
const title = computed(() => isTemperature.value ? '2 米气温趋势' : '降水量趋势')
const unit = computed(() => isTemperature.value ? '℃' : 'mm')
let chart, observer
function update() {
  if (!chart) return
  try {
    const builder = isTemperature.value ? buildTemperatureOption : buildPrecipitationOption
    chart.setOption(builder(props.points), { notMerge: true })
    chartFailed.value = false
  } catch { chartFailed.value = true }
}
function resize() {
  if (!chart) return
  try { chart.resize() } catch { chartFailed.value = true }
}
function release() {
  observer?.disconnect(); observer = null
  window.removeEventListener('resize', resize)
  chart?.dispose(); chart = null
}
function mountChart() {
  release()
  try {
    chart = init(container.value)
    observer = new ResizeObserver(resize)
    observer.observe(container.value)
    window.addEventListener('resize', resize)
    update()
  } catch { chartFailed.value = true }
}
onMounted(mountChart)
watch(() => [props.points, props.kind], update)
onBeforeUnmount(release)
</script>

<template>
  <section class="trend-chart-panel" :aria-label="title">
    <header><div><span class="chart-marker" :class="{ rain: !isTemperature }" /><h2>{{ title }}</h2><span class="chart-code">{{ isTemperature ? 'T2M / LINE' : 'PRECIP / BAR' }}</span></div><span class="sample-count">{{ points.length }} 个样本 · {{ unit }}</span></header>
    <div class="chart-body">
      <div ref="container" class="trend-chart-canvas" role="img" :aria-label="`${title}，${points.length} 个样本，单位 ${unit}`" />
      <div v-if="chartFailed" class="chart-overlay" role="alert"><p>图表暂时无法显示</p><button @click="mountChart">重新绘制图表</button></div>
      <div v-else-if="!points.length" class="chart-overlay" role="status"><p>{{ status === 'loading' || status === 'idle' ? '正在加载趋势数据…' : status === 'error' ? '等待重新查询' : '当前要素暂无数据' }}</p></div>
    </div>
  </section>
</template>

<style scoped>
.trend-chart-panel { min-width: 0; padding: 16px 20px 8px; border: 1px solid #36515f; border-radius: 10px; background: #132b3a; }
header, header > div { display: flex; align-items: center; gap: 10px; } header { justify-content: space-between; flex-wrap: wrap; gap: 8px; }
h2 { font-size: 14px; font-weight: 500; margin: 0; color: #e2edf1; }.chart-code { font-size: 9px; color: #7e9aaa; letter-spacing: .12em; }
.chart-marker { height: 12px; width: 3px; border-radius: 3px; background: #d5dba3; }.chart-marker.rain { background: #70b1cf; }
.sample-count { font-size: 10px; color: #94afbd; }.chart-body { position: relative; }
.trend-chart-canvas { height: clamp(190px, 24vh, 285px); width: 100%; }
.chart-overlay { position: absolute; inset: 15px 0 35px; display: flex; align-items: center; justify-content: center; flex-direction: column; background: #132b3ad9; color: #9eb9c8; font-size: 13px; }
.chart-overlay button { cursor: pointer; border: 1px solid #52778a; border-radius: 5px; padding: 7px 14px; background: #254556; color: #e2edf1; }
@media(max-width: 650px) { .trend-chart-panel { padding: 14px 10px 4px; }.chart-code { display: none; } }
</style>
