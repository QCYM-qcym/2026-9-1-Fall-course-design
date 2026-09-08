<script setup>
import { computed, onMounted, onBeforeUnmount } from 'vue'
import { fetchCities } from '../api/city.js'
import { fetchForecastModels } from '../api/forecastModel.js'
import { fetchTrend } from '../api/weather.js'
import { createTrendState } from '../utils/trendState.js'
import TrendFilterBar from '../components/analysis/TrendFilterBar.vue'
import TrendStatistics from '../components/analysis/TrendStatistics.vue'
import TrendChart from '../components/analysis/TrendChart.vue'

const trend = createTrendState({ fetchCities, fetchForecastModels, fetchTrend })
const { state } = trend
const changed = computed(() => state.appliedQuery && (state.cityId !== state.appliedQuery.cityId ||
  state.modelId !== state.appliedQuery.modelId || state.range?.[0] !== state.appliedQuery.startTime || state.range?.[1] !== state.appliedQuery.endTime))
onMounted(trend.initialize)
onBeforeUnmount(trend.dispose)
</script>

<template>
  <section class="trend-workspace" aria-label="气象趋势分析" :aria-busy="state.status === 'loading'">
    <header class="trend-heading"><div><p class="trend-eyebrow">SHANDONG / TREND ANALYSIS</p><h1>趋势分析<span>一座城市，两种气象视角。</span></h1></div><span class="trend-demo">课程演示 · 合成数据</span></header>
    <div class="trend-filter-panel">
      <TrendFilterBar :cities="state.cities" :models="state.models" :city-id="state.cityId" :model-id="state.modelId" :range="state.range"
        :ready="state.dictionariesReady" :loading="state.status === 'loading'" @city="state.cityId = $event" @model="state.modelId = $event"
        @range="state.range = $event" @query="trend.load" />
    </div>
    <div class="trend-result-heading" role="status">
      <span v-if="state.response"><strong>{{ state.response.cityName }}</strong> / {{ state.response.modelName }} <span class="result-time">{{ state.appliedQuery.startTime }} — {{ state.appliedQuery.endTime }}</span></span>
      <span v-else>{{ state.status === 'loading' ? (state.dictionariesReady ? '正在查询趋势数据…' : '正在加载城市与模型…') : '选择条件，查看城市气象趋势' }}</span>
      <span v-if="changed" class="draft-note">筛选已修改 · 点击查询后更新</span>
      <span v-else-if="state.response" class="trend-source">● API 数据</span>
    </div>
    <p v-if="state.warning" class="trend-warning" role="status">{{ state.warning }}</p>
    <div v-if="state.status === 'error'" class="trend-state error" role="alert"><div><strong>暂时无法加载趋势</strong><p>{{ state.error }}</p></div><button @click="trend.retry">重试</button></div>
    <div v-else-if="state.status === 'empty'" class="trend-state" role="status"><strong>当前条件暂无趋势数据</strong><span>请调整城市、模型或时间范围后查询。</span></div>
    <TrendStatistics :statistics="state.response?.statistics ?? null" />
    <div class="trend-charts">
      <TrendChart kind="temperature" :points="state.response?.temperature ?? []" :status="state.status" />
      <TrendChart kind="precipitation" :points="state.response?.precipitation ?? []" :status="state.status" />
    </div>
    <footer class="trend-footnote"><span>统计仅基于接口返回样本 · 缺测不补零 · 各要素时次独立</span><span>本地业务时间 / Asia·Shanghai</span></footer>
  </section>
</template>

<style scoped>
.trend-workspace { color: #e3edf0; --el-fill-color-blank: #203846; --el-text-color-regular: #d2e0e8; --el-text-color-primary: #e3edf0; --el-text-color-placeholder: #91a9b7; --el-border-color: #466170; --el-disabled-bg-color: #203543; --el-disabled-text-color: #94abb8; }
.trend-heading { display: flex; align-items: center; justify-content: space-between; gap: 14px; margin-bottom: 23px; }
.trend-eyebrow { margin: 0 0 8px; font-size: 10px; letter-spacing: .16em; color: #8fb2c5; }
h1 { margin: 0; font-size: 28px; font-weight: 500; letter-spacing: .06em; } h1 span { margin-left: 18px; font-size: 12px; letter-spacing: .02em; font-weight: 400; color: #9db6c4; }
.trend-demo { padding: 6px 9px; border: 1px solid #615f44; border-radius: 5px; color: #cfcca9; font-size: 10px; white-space: nowrap; }
.trend-filter-panel { border: 1px solid #385564; border-radius: 10px; background: #172f3e; padding: 19px 22px; }
.trend-result-heading { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; min-height: 50px; padding: 10px 2px; color: #afc5d0; font-size: 11px; }
.trend-result-heading strong { font-weight: 500; color: #e0ebef; font-size: 13px; margin-right: 8px; }.result-time { margin-left: 18px; color: #90aebf; }.trend-source { color: #a5bf99; font-size: 10px; }
.draft-note, .trend-warning { color: #e1cd97; font-size: 11px; }.trend-warning { margin: 0 0 12px; }
.trend-charts { display: grid; gap: 16px; margin-top: 18px; }
.trend-state { border: 1px solid #476371; background: #1c3544; border-radius: 8px; margin-bottom: 14px; padding: 14px 18px; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; font-size: 12px; color: #b8ccd7; }
.trend-state strong { font-size: 14px; color: #e1e9e9; font-weight: 500; }.trend-state p { margin: 5px 0 0; line-height: 1.6; }.trend-state.error { border-color: #7b6251; }
.trend-state button { padding: 8px 22px; background: #334c57; color: #e6edf1; border: 1px solid #66818d; border-radius: 5px; cursor: pointer; }
.trend-state button:focus-visible { outline: 2px solid #e7dfa2; outline-offset: 3px; }
.trend-footnote { display: flex; justify-content: space-between; gap: 10px; flex-wrap: wrap; margin-top: 15px; font-size: 10px; line-height: 1.7; color: #829ead; }
@media(max-width: 760px) { .trend-heading { align-items: flex-start; } h1 span { display: block; margin: 7px 0 0; } h1 { font-size: 24px; }.trend-filter-panel { padding: 16px; }.result-time { display: block; margin: 5px 0 0; } }
</style>
