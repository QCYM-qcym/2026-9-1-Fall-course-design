<script setup>
import { computed, onMounted, onBeforeUnmount } from 'vue'
import { fetchCities } from '../api/city.js'
import { fetchWeatherElements } from '../api/weatherElement.js'
import { fetchComparison } from '../api/weather.js'
import { createComparisonState } from '../utils/comparisonState.js'
import { COMPARISON_MODELS } from '../utils/comparisonAnalysis.js'
import ComparisonChart from '../components/comparison/ComparisonChart.vue'

const comparison = createComparisonState({ fetchCities, fetchWeatherElements, fetchComparison })
const { state } = comparison
const changed = computed(() => state.appliedQuery && (state.cityId !== state.appliedQuery.cityId ||
  state.elementId !== state.appliedQuery.elementId || state.range?.[0] !== state.appliedQuery.startTime || state.range?.[1] !== state.appliedQuery.endTime))
const summaries = computed(() => COMPARISON_MODELS.map(code => ({ code,
  count: state.response?.series.find(series => series.modelCode === code)?.values.length ?? null })))
onMounted(comparison.initialize)
onBeforeUnmount(comparison.dispose)
</script>

<template>
  <section class="comparison-workspace" aria-label="气象模型对比" :aria-busy="state.status === 'loading'">
    <header class="heading"><div><p class="eyebrow">SHANDONG / MODEL COMPARISON</p><h1>模型对比</h1><p class="subtitle">同一城市，同一要素，观察两个模型的预报。</p></div><span class="demo">课程演示 · 合成数据</span></header>
    <form class="filters" aria-label="模型对比查询条件" @submit.prevent="comparison.load">
      <label><span>城市 / CITY</span><select v-model.number="state.cityId" aria-label="对比城市" :disabled="!state.dictionariesReady"><option v-if="!state.cities.length" :value="null" disabled>等待城市字典</option><option v-for="city in state.cities" :key="city.id" :value="city.id">{{ city.cityName }}</option></select></label>
      <label><span>气象要素 / ELEMENT</span><select v-model.number="state.elementId" aria-label="对比要素" :disabled="!state.dictionariesReady"><option v-if="!state.elements.length" :value="null" disabled>等待要素字典</option><option v-for="element in state.elements" :key="element.id" :value="element.id">{{ element.elementCode }} · {{ element.elementName }}</option></select></label>
      <div class="range"><span id="comparison-range-label">时间范围 / 本地业务时间</span><el-date-picker v-model="state.range" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm" start-placeholder="开始时间" end-placeholder="结束时间" range-separator="—" :clearable="false" :disabled="!state.dictionariesReady" aria-labelledby="comparison-range-label" /></div>
      <button type="submit" :disabled="!state.dictionariesReady">{{ state.status === 'loading' && state.dictionariesReady ? '重新查询' : '查询对比' }} ↗</button>
    </form>
    <div class="result-heading" role="status"><span v-if="state.response"><strong>{{ state.response.cityName }}</strong> / {{ state.response.element.elementCode }} · {{ state.response.element.unit }}<span class="result-range">{{ state.appliedQuery.startTime }} — {{ state.appliedQuery.endTime }}</span></span><span v-else>{{ state.status === 'loading' ? '正在加载模型对比…' : '选择条件，查看模型预报' }}</span><span v-if="changed" class="warning">筛选已修改 · 点击查询后更新</span></div>
    <p v-if="state.warning" class="warning" role="status">{{ state.warning }}</p>
    <div v-if="state.status === 'error'" class="notice error" role="alert"><div><strong>暂时无法加载模型对比</strong><p>{{ state.error }}</p></div><button @click="comparison.retry">重试</button></div>
    <div v-else-if="state.status === 'empty'" class="notice" role="status">当前条件暂无对比数据，请调整城市、要素或时间范围。</div>
    <div class="model-summaries"><div v-for="model in summaries" :key="model.code" class="model-summary" :class="model.code.toLowerCase()"><strong>{{ model.code }}</strong><span>{{ model.code === 'ECMWF' ? '实线 · 圆点' : '虚线 · 菱形' }}</span><span>{{ model.count === null ? '--' : model.count }} 个样本</span></div></div>
    <ComparisonChart :data="state.response" :status="state.status" />
    <footer>两个模型按预报时间并集展示 · 缺测断开，不补零 · 不代表模型准确率评价<span>本地业务时间 / Asia·Shanghai</span></footer>
  </section>
</template>

<style scoped>
.comparison-workspace { color: #e3edf0; --el-fill-color-blank: #203846; --el-text-color-regular: #d2e0e8; --el-text-color-primary: #e3edf0; --el-text-color-placeholder: #91a9b7; --el-border-color: #466170; --el-disabled-bg-color: #203543; --el-disabled-text-color: #94abb8; }
.heading { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 20px; }.eyebrow { color: #8fb2c5; letter-spacing: .16em; font-size: 10px; margin: 0 0 8px; }h1 { margin: 0; font-size: 28px; font-weight: 500; }.subtitle { font-size: 12px; color: #9db6c4; margin: 8px 0 0; }.demo { border: 1px solid #615f44; border-radius: 5px; padding: 6px 9px; font-size: 10px; color: #cfcca9; white-space: nowrap; }
.filters { display: grid; grid-template-columns: minmax(120px, 1fr) minmax(160px, 1.2fr) minmax(350px, 2.8fr) auto; gap: 16px; align-items: end; padding: 18px 20px; background: #172f3e; border: 1px solid #385564; border-radius: 10px; }.filters label, .range { display: grid; gap: 9px; min-width: 0; }.filters label > span, .range > span { font-size: 10px; color: #9db5c3; letter-spacing: .06em; }select { width: 100%; min-width: 0; height: 38px; padding: 0 10px; background: #203846; color: #e3edf0; border: 1px solid #466170; border-radius: 6px; font: inherit; font-size: 13px; }.range :deep(.el-date-editor) { width: 100%; height: 38px; box-sizing: border-box; }.range :deep(.el-range-input) { min-width: 0; font-size: 12px; }
button { min-height: 38px; padding: 0 18px; border: 1px solid #d1dba7; border-radius: 6px; background: #d1dba7; color: #14282d; font-weight: 600; cursor: pointer; }button:disabled, select:disabled { opacity: .5; cursor: not-allowed; }button:focus-visible, select:focus-visible { outline: 2px solid #e7dfa2; outline-offset: 3px; }.result-heading { display: flex; justify-content: space-between; flex-wrap: wrap; gap: 8px; padding: 14px 2px; min-height: 46px; font-size: 12px; color: #afc5d0; }.result-range { margin-left: 16px; font-size: 11px; }.warning { color: #e1cd97; font-size: 11px; }.notice { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 14px 18px; border: 1px solid #476371; border-radius: 8px; margin-bottom: 14px; font-size: 12px; color: #b8ccd7; }.notice p { margin-bottom: 0; }.error { border-color: #7b6251; }
.model-summaries { display: flex; gap: 14px; margin-bottom: 14px; }.model-summary { display: flex; align-items: center; flex-wrap: wrap; gap: 16px; flex: 1; padding: 13px 18px; border: 1px solid #385564; border-left: 3px solid #d5dba3; border-radius: 6px; background: #172f3e; font-size: 11px; color: #a6bbc7; }.model-summary strong { color: #d5dba3; font-size: 13px; font-weight: 500; }.noaa { border-left-color: #70b1cf; }.noaa strong { color: #70b1cf; }footer { display: flex; justify-content: space-between; flex-wrap: wrap; gap: 8px; margin-top: 13px; font-size: 10px; line-height: 1.7; color: #829ead; }
@media(max-width: 1000px) { .filters { grid-template-columns: 1fr 1fr; }.range { grid-column: 1 / -1; }.filters button { grid-column: 1 / -1; } }
@media(max-width: 650px) { .heading { align-items: flex-start; }.demo { white-space: normal; }.filters { padding: 14px; }.model-summaries { flex-direction: column; }.result-range { display: block; margin: 6px 0 0; } }
</style>
