<script setup>
import { computed } from 'vue'
import { TEMPERATURE_COLORS, PRECIPITATION_COLORS } from '../../utils/weatherWorkbench.js'
const props = defineProps({ range: Object, element: Object })
const colors = computed(() => props.element?.elementCode === 'PRECIP' ? PRECIPITATION_COLORS : TEMPERATURE_COLORS)
</script>
<template>
  <section class="weather-legend floating-panel" aria-label="当前时次图例">
    <div class="legend-title"><strong>{{ element?.elementName ?? '数值图例' }}</strong><span>{{ element?.unit ?? '—' }}</span></div>
    <div class="color-ramp" :style="{ background: `linear-gradient(to right, ${colors.join(',')})` }" />
    <div class="legend-range"><span>{{ range.empty ? '—' : range.min.toFixed(2) }}</span><span>{{ range.empty ? '—' : range.max.toFixed(2) }}</span></div>
    <p><i /> 暂无数据 <span>· 当前时次动态色标</span></p>
  </section>
</template>
<style scoped>
.weather-legend { position: absolute; right: 24px; bottom: 145px; width: 230px; padding: 15px 17px; }
.legend-title, .legend-range { display: flex; justify-content: space-between; font-size: 12px; }
.legend-title span { color: #a2beca; }
.color-ramp { height: 9px; border-radius: 5px; margin: 13px 0 7px; }
.legend-range { font-variant-numeric: tabular-nums; color: #bcd0d7; }
p { display: flex; align-items: center; gap: 5px; margin: 12px 0 0; font-size: 10px; color: #b5c8cf; }
p span { color: #8eabb7; } i { width: 9px; height: 9px; background: #314655; border: 1px solid #77909d; }
@media(max-width: 1000px) { .weather-legend { right: 12px; width: 200px; bottom: 146px; } }
</style>
