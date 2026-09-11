<script setup>
import { computed } from 'vue'
import { WEATHER_ELEMENTS, formatElementNumber } from '../../utils/weatherElements.js'
const props = defineProps({ range: Object, element: Object })
const meta = computed(() => WEATHER_ELEMENTS[props.element?.elementCode])
const colors = computed(() => meta.value?.colors ?? WEATHER_ELEMENTS.T2M.colors)
</script>
<template>
  <section class="weather-legend floating-panel" aria-label="当前时次图例">
    <div class="legend-title"><strong>{{ meta?.name ?? element?.elementName ?? '数值图例' }}</strong><span>{{ meta?.unit ?? element?.unit ?? '—' }}</span></div>
    <div class="color-ramp" :style="{ background: `linear-gradient(to right, ${colors.join(',')})` }" />
    <div class="legend-range"><span>min {{ range.empty ? '—' : formatElementNumber(range.min, element?.elementCode) }}</span><span>max {{ range.empty ? '—' : formatElementNumber(range.max, element?.elementCode) }}</span></div>
    <div v-if="meta?.displayType === 'direction'" class="direction-key">0° 北 · 90° 东 · 180° 南<br />270° 西 · 360° 北（环形方位）</div>
    <p><i /> 暂无数据 <span>· {{ meta?.displayType === 'direction' ? '颜色表示方位' : meta?.displayType === 'percentage' ? '固定 0–100%' : '当前时次动态色标' }}</span></p>
  </section>
</template>
<style scoped>
.weather-legend { position: relative; flex-shrink: 0; width: 100%; padding: 15px 17px; }
.legend-title, .legend-range { display: flex; justify-content: space-between; font-size: 12px; }
.legend-title span { color: #a2beca; }
.color-ramp { height: 9px; border-radius: 5px; margin: 13px 0 7px; }
.legend-range { font-variant-numeric: tabular-nums; color: #bcd0d7; }
.direction-key { margin-top: 8px; color: #c0d1da; font-size: 11px; line-height: 1.7; }
p { display: flex; align-items: center; gap: 5px; margin: 12px 0 0; font-size: 10px; color: #b5c8cf; }
p span { color: #8eabb7; } i { width: 9px; height: 9px; background: #314655; border: 1px solid #77909d; }
</style>
