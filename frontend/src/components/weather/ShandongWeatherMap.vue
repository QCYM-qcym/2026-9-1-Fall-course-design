<script setup>
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { init, registerMap, use } from 'echarts/core'
import { MapChart } from 'echarts/charts'
import { TooltipComponent, VisualMapContinuousComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import geo from '../../assets/maps/shandong.json'
import { createMapOption } from '../../utils/mapOption.js'

use([MapChart, TooltipComponent, VisualMapContinuousComponent, CanvasRenderer])
registerMap('shandong-course', geo)
const props = defineProps({ rows: { type: Array, required: true }, range: { type: Object, required: true }, elementCode: String, unit: String, selectedCityId: Number })
const emit = defineEmits(['select', 'error'])
const container = ref(null)
let chart, observer
function update() {
  if (!chart) return
  try { chart.setOption(createMapOption(props.rows, props.range, props.elementCode, props.unit, props.selectedCityId), { notMerge: true }) }
  catch { emit('error') }
}
onMounted(() => {
  try {
    chart = init(container.value)
    chart.on('click', params => {
      const city = props.rows.find(row => row.name === params.name)
      if (city) emit('select', city.id)
    })
    observer = new ResizeObserver(() => chart?.resize())
    observer.observe(container.value)
    update()
  } catch { emit('error') }
})
watch(() => [props.rows, props.range, props.elementCode, props.unit, props.selectedCityId], update)
onBeforeUnmount(() => { observer?.disconnect(); chart?.dispose(); chart = null })
</script>

<template>
  <div ref="container" class="shandong-map" role="img" aria-label="山东省 16 市气象分布地图，点击城市查看详情；也可使用右侧城市选择框" />
</template>

<style scoped>
.shandong-map { position: absolute; inset: 112px 272px 145px 178px; }
@media (min-width: 1600px) { .shandong-map { inset: 125px 290px 150px 190px; } }
@media (max-width: 1000px) { .shandong-map { inset: 135px 12px 320px; } }
</style>
