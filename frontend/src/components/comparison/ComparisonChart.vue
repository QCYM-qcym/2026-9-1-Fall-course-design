<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { init, use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { buildComparisonOption, isComparisonEmpty } from '../../utils/comparisonAnalysis.js'

use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])
const props = defineProps({ data: { type: Object, default: null }, status: String })
const container = ref(null)
const failed = ref(false)
let chart, observer
function update() {
  if (!chart) return
  try { chart.setOption(buildComparisonOption(props.data), { notMerge: true }); failed.value = false }
  catch { failed.value = true }
}
function resize() {
  if (!chart) return
  try { chart.resize() } catch { failed.value = true }
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
  } catch { failed.value = true }
}
onMounted(mountChart)
watch(() => props.data, update)
onBeforeUnmount(release)
</script>

<template>
  <section class="comparison-chart" aria-label="ECMWF 与 NOAA 双折线对比">
    <header><h2>{{ data?.element.elementName ?? '模型预报' }}对比</h2><span>ECMWF / NOAA · {{ data?.element.unit ?? '--' }}</span></header>
    <div class="chart-body">
      <div ref="container" class="canvas" role="img" :aria-label="`${data?.cityName ?? ''} ${data?.element.elementCode ?? ''} ECMWF 与 NOAA 双折线，单位 ${data?.element.unit ?? '--'}`" />
      <div v-if="failed" class="overlay" role="alert"><p>图表暂时无法显示</p><button @click="mountChart">重新绘制图表</button></div>
      <div v-else-if="!data || isComparisonEmpty(data)" class="overlay" role="status">{{ status === 'loading' || status === 'idle' ? '正在加载模型预报…' : status === 'error' ? '等待重新查询' : '当前条件暂无对比数据' }}</div>
    </div>
  </section>
</template>

<style scoped>
.comparison-chart { min-width: 0; padding: 16px 20px 8px; border: 1px solid #36515f; border-radius: 10px; background: #132b3a; }header { display: flex; justify-content: space-between; align-items: center; gap: 10px; flex-wrap: wrap; }h2 { margin: 0; font-size: 14px; font-weight: 500; color: #e2edf1; }header span { font-size: 10px; color: #94afbd; }.chart-body { position: relative; }.canvas { width: 100%; height: clamp(300px, 43vh, 490px); }.overlay { position: absolute; inset: 45px 0 35px; display: flex; align-items: center; justify-content: center; flex-direction: column; background: #132b3ae6; color: #9eb9c8; font-size: 13px; }.overlay button { cursor: pointer; border: 1px solid #52778a; border-radius: 5px; padding: 7px 14px; background: #254556; color: #e2edf1; }
@media(max-width: 650px) { .comparison-chart { padding: 14px 8px 4px; } }
</style>
