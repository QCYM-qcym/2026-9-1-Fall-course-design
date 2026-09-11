<script setup>
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import geo from '../assets/maps/shandong.json'
import { fetchCities } from '../api/city.js'
import { fetchForecastModels } from '../api/forecastModel.js'
import { fetchWeatherElements } from '../api/weatherElement.js'
import { fetchWorkbench } from '../api/weather.js'
import { createWorkbenchState } from '../utils/workbenchState.js'
import { DEMO_RANGE, calculateLegendRange, joinCityWeatherData } from '../utils/weatherWorkbench.js'
import ShandongWeatherMap from '../components/weather/ShandongWeatherMap.vue'
import WeatherToolbar from '../components/weather/WeatherToolbar.vue'
import WeatherSwitchers from '../components/weather/WeatherSwitchers.vue'
import WeatherLegend from '../components/weather/WeatherLegend.vue'
import CityDetailPanel from '../components/weather/CityDetailPanel.vue'
import ForecastTimeline from '../components/weather/ForecastTimeline.vue'

const workbench = createWorkbenchState({ fetchCities, fetchForecastModels, fetchWeatherElements, fetchWorkbench }, geo)
const { state } = workbench
const mapError = ref(false)
const model = computed(() => state.models.find(item => item.id === state.modelId))
const element = computed(() => state.elements.find(item => item.id === state.elementId))
const rows = computed(() => joinCityWeatherData(state.cities, state.response?.records ?? [], state.selectedTime))
const legend = computed(() => calculateLegendRange(rows.value.map(row => row.value), element.value?.elementCode))
const selectedCity = computed(() => rows.value.find(row => row.id === state.selectedCityId) ?? null)
const valueCount = computed(() => rows.value.filter(row => Number.isFinite(row.value)).length)
function retry() { return state.dictionariesReady ? workbench.load() : workbench.initialize() }
onMounted(workbench.initialize)
onBeforeUnmount(workbench.dispose)
</script>

<template>
  <section class="weather-workspace" :aria-busy="state.status === 'loading'" aria-label="山东气象工作台">
    <div class="ocean-label">黄海 <span>YELLOW SEA</span></div>
    <ShandongWeatherMap :rows="rows" :range="legend" :element-code="element?.elementCode" :unit="element?.unit" :selected-city-id="state.selectedCityId" @select="state.selectedCityId = $event" @error="mapError = true" />
    <WeatherToolbar :model="model" :element="element" :range="state.range" :disabled="!state.dictionariesReady" @range="workbench.load({ range: $event })" />
    <WeatherSwitchers :models="state.models" :elements="state.elements" :model-id="state.modelId" :element-id="state.elementId" :disabled="!state.dictionariesReady" @model="workbench.load({ modelId: $event })" @element="workbench.load({ elementId: $event })" />
    <div class="weather-inspector">
      <CityDetailPanel :city="selectedCity" :cities="state.cities" :model="model" :element="element" :time="state.selectedTime" :ready="state.status === 'success'" @select="state.selectedCityId = $event" @close="state.selectedCityId = null" />
      <WeatherLegend :range="legend" :element="element" />
    </div>
    <div v-if="state.status === 'loading' || state.status === 'idle'" class="query-state floating-panel" role="status"><span class="loading-dot" /><h2>正在加载预报数据</h2><p>读取字典与当前查询范围，请稍候。</p></div>
    <div v-else-if="mapError || state.status === 'error'" class="query-state floating-panel" role="alert"><span class="state-symbol">!</span><h2>{{ mapError ? '地图暂时无法显示' : '暂时无法加载' }}</h2><p>{{ mapError ? '请刷新页面重试，或检查浏览器绘图支持。' : state.error }}</p><button v-if="!mapError" @click="retry">重新加载</button></div>
    <div v-else-if="state.status === 'empty'" class="query-state floating-panel" role="status"><span class="state-symbol">—</span><h2>此范围暂无预报记录</h2><p>{{ model?.modelName }} · {{ element?.elementName }}</p><p>{{ state.range[0] }} — {{ state.range[1] }}</p><button @click="workbench.load({ range: [...DEMO_RANGE] })">返回课程演示范围</button></div>
    <p v-if="state.warning" class="data-warning" role="status">{{ state.warning }}</p>
    <div class="map-status" role="status"><span class="status-dot" :class="{ ready: state.status === 'success' }" />{{ state.status === 'success' ? `${valueCount} / 16 市有值` : '等待有效数据' }}<span>{{ state.matched ? `边界匹配 ${state.matched}/16` : '16 市边界已载入 · 字典待验证' }}</span><span v-if="state.source">{{ state.source === 'cache' ? '内存缓存' : 'API 数据' }}</span></div>
    <ForecastTimeline :times="state.response?.times ?? []" :selected-time="state.selectedTime" :disabled="state.status !== 'success'" @select="workbench.selectTime" />
    <p class="map-attribution">边界：China-GeoData · MIT · 仅课程示意，不用于测绘</p>
  </section>
</template>

<style>
.weather-workspace {
  position: relative; min-height: 650px; height: calc(100vh - 92px); overflow: hidden;
  color: #e5eef2; background: radial-gradient(ellipse at 47% 43%, #214454 0, #152d3a 45%, #10222f 85%);
  --el-fill-color-blank: #1d3544; --el-text-color-regular: #d1e0e7; --el-text-color-primary: #e5eef2;
  --el-border-color: #47616d; --el-disabled-bg-color: #213746; --el-disabled-text-color: #90a6b1;
}
.weather-workspace::before { content: ''; position: absolute; inset: 0; opacity: .15; pointer-events: none; background-image: linear-gradient(#557d8d33 1px, transparent 1px), linear-gradient(90deg, #557d8d33 1px, transparent 1px); background-size: 68px 68px; }
.weather-inspector { position: absolute; top: 152px; bottom: 145px; right: 24px; width: 230px; display: flex; flex-direction: column; gap: 12px; }
.weather-workspace .floating-panel { z-index: 2; background: #122735ed; border: 1px solid #365361; border-radius: 10px; box-shadow: 0 8px 30px #050f1826; backdrop-filter: blur(12px); }
.weather-workspace .micro-label { margin: 0; color: #93b1c1; font-size: 10px; letter-spacing: .13em; font-weight: 500; }
.weather-workspace .muted { font-size: 12px; color: #99b3bf; }
.weather-workspace button { font: inherit; font-size: 12px; color: #c1d5df; border: 1px solid #3c5665; border-radius: 6px; background: #203744; cursor: pointer; transition: background .2s, border-color .2s; }
.weather-workspace button:hover:not(:disabled) { border-color: #8eb8c7; background: #304d5d; }
.weather-workspace button[aria-pressed="true"] { background: #395d65; color: #edfadf; border-color: #8aa88e; box-shadow: inset 3px 0 #c6d79e; }
.weather-workspace button:focus-visible, .weather-workspace select:focus-visible { outline: 2px solid #e4daa3; outline-offset: 3px; }
.weather-workspace button:disabled { opacity: .5; cursor: not-allowed; }
.ocean-label { position: absolute; left: 64%; top: 61%; color: #6e95a3; letter-spacing: .8em; font-size: 18px; pointer-events: none; opacity: .6; }
.ocean-label span { display: block; font-size: 9px; letter-spacing: .25em; margin-top: 10px; }
.query-state { position: absolute; left: 47%; top: 47%; transform: translate(-50%, -50%); width: 350px; max-width: calc(100% - 40px); padding: 24px; text-align: center; }
.query-state h2 { font-size: 18px; font-weight: 500; margin: 12px 0; }
.query-state p { color: #afc5d0; font-size: 12px; line-height: 1.8; margin: 7px 0; }
.query-state button { margin-top: 13px; padding: 9px 22px; }
.state-symbol { display: inline-flex; width: 28px; height: 28px; justify-content: center; align-items: center; border: 1px solid #b9af82; color: #e1cc8f; border-radius: 50%; }
.loading-dot { display: inline-block; width: 18px; height: 18px; border-radius: 50%; border: 2px solid #4d7080; border-top-color: #c3d6aa; animation: weather-spin 1s linear infinite; }
@keyframes weather-spin { to { transform: rotate(360deg); } }
@media(prefers-reduced-motion: reduce) { .loading-dot { animation: none; } }
.map-status { position: absolute; left: 220px; bottom: 146px; display: flex; align-items: center; gap: 9px; font-size: 11px; color: #bdcdd4; }
.map-status > span:not(.status-dot) { border-left: 1px solid #5f7d8b; padding-left: 9px; color: #92adba; }
.status-dot { width: 6px; height: 6px; border-radius: 50%; background: #a79b7d; }.status-dot.ready { background: #b8d794; }
.map-attribution { position: absolute; right: 25px; bottom: 4px; margin: 0; font-size: 9px; color: #8ca8b5; }
.data-warning { position: absolute; left: 195px; top: 126px; color: #e5cf94; font-size: 12px; }
@media(max-width: 1000px) { .weather-workspace { min-height: 840px; }.map-status { left: 18px; bottom: 334px; }.query-state { left: 44%; top: 40%; } }
@media(max-width: 1000px) { .weather-inspector { right: 12px; width: 205px; top: 152px; bottom: 165px; } }
</style>
