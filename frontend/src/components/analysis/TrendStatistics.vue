<script setup>
import { computed } from 'vue'
import { buildTrendStatistics } from '../../utils/trendAnalysis.js'
const props = defineProps({ statistics: Object })
const cards = computed(() => buildTrendStatistics(props.statistics))
</script>

<template>
  <section class="trend-statistics" aria-label="趋势统计">
    <article v-for="card in cards" :key="card.key" class="trend-metric" :class="{ precipitation: card.key === 'precipitationTotal' }">
      <div class="metric-heading"><span>{{ card.label }}</span><span class="metric-mark" aria-hidden="true">{{ card.key === 'precipitationTotal' ? '◒' : '°' }}</span></div>
      <p class="metric-value">{{ card.value }} <small>{{ card.unit }}</small></p>
      <p class="metric-caption">{{ card.caption }}</p>
    </article>
  </section>
</template>

<style scoped>
.trend-statistics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.trend-metric { padding: 15px 20px; border: 1px solid #36515f; border-radius: 9px; background: #162e3d; min-width: 0; }
.metric-heading { display: flex; align-items: center; justify-content: space-between; color: #b8cbd4; font-size: 12px; }
.metric-mark { color: #d5dba3; font-size: 20px; height: 24px; }.precipitation .metric-mark { color: #86bdd5; }
.metric-value { margin: 5px 0 7px; font-size: clamp(25px, 2.8vw, 34px); font-weight: 300; color: #e0e8c6; font-variant-numeric: tabular-nums; }
.metric-value small { font-size: 13px; color: #a9c0cc; }.precipitation .metric-value { color: #b8dfed; }
.metric-caption { margin: 0; font-size: 9px; letter-spacing: .09em; color: #819dab; }
@media(max-width: 650px) { .trend-statistics { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }.trend-metric { padding: 12px; } }
</style>
