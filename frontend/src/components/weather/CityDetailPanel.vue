<script setup>
import { formatWeatherValue } from '../../utils/mapOption.js'
import { windDirection } from '../../utils/weatherElements.js'
defineProps({ city: Object, cities: Array, model: Object, element: Object, time: String, ready: Boolean })
defineEmits(['select', 'close'])
</script>
<template>
  <aside class="city-detail floating-panel" aria-label="城市详情">
    <div class="detail-heading"><span class="micro-label">CITY INSPECTOR</span><button v-if="city" class="close-button" aria-label="关闭城市详情" @click="$emit('close')">×</button></div>
    <select aria-label="选择城市查看详情" :value="city?.id ?? ''" @change="$emit('select', Number($event.target.value))">
      <option disabled value="">点击地图或选择城市</option>
      <option v-for="item in cities" :key="item.id" :value="item.id">{{ item.cityName }}</option>
    </select>
    <template v-if="city">
      <p class="city-value" :class="{ 'no-value': !ready || !Number.isFinite(city.value) }">{{ ready ? formatWeatherValue(city.value, element?.elementCode) : '等待查询结果' }}<small v-if="ready && Number.isFinite(city.value)">{{ element?.unit }}</small></p>
      <p v-if="ready && element?.elementCode === 'WIND_DIR_100M' && Number.isFinite(city.value)" class="wind-bearing">{{ windDirection(city.value) }}风 · 气象风向</p>
      <dl>
        <div><dt>气象要素</dt><dd>{{ element?.elementName ?? '—' }}</dd></div>
        <div><dt>预报模型</dt><dd>{{ model?.modelName ?? '—' }}</dd></div>
        <div><dt>预报时间</dt><dd>{{ time || '—' }}</dd></div>
        <div><dt>经度</dt><dd>{{ city.longitude }}° E</dd></div>
        <div><dt>纬度</dt><dd>{{ city.latitude }}° N</dd></div>
      </dl>
    </template>
    <p v-else class="detail-hint">探索一座城市<br /><span>选择地图区域，查看该时次的预报演示值。</span></p>
  </aside>
</template>
<style scoped>
.city-detail { position: relative; flex: 1; min-height: 0; width: 100%; padding: 14px; overflow-y: auto; }
.detail-heading { display: flex; justify-content: space-between; align-items: center; height: 24px; margin-bottom: 10px; }
.close-button { font-size: 22px; line-height: 20px; padding: 0 6px; }
select { width: 100%; padding: 8px 5px; font: inherit; font-size: 12px; background: #213a49; color: #e6eef2; border: 1px solid #436070; border-radius: 5px; }
.city-value { font-size: 32px; line-height: 1.15; font-weight: 300; margin: 8px 0; font-variant-numeric: tabular-nums; color: #e4eecf; }
.city-value small { font-size: 17px; margin-left: 8px; color: #bad0d9; }
.city-value.no-value { font-size: 18px; margin-top: 12px; }
.wind-bearing { margin: 0 0 12px; color: #d7e2bc; font-size: 14px; }
dl { margin: 0; display: grid; gap: 5px; font-size: 11px; line-height: 14px; }
dl div { display: flex; justify-content: space-between; gap: 6px; } dt { color: #93acba; flex-shrink: 0; } dd { margin: 0; text-align: right; }
.detail-hint { font-size: 17px; line-height: 1.9; margin: 25px 0 10px; }
.detail-hint span { display: block; margin-top: 6px; font-size: 12px; color: #a1bcc9; line-height: 1.8; }
@media(max-width: 1000px) { .city-detail { padding: 12px; } .city-value { font-size: 30px; } dl { gap: 6px; } }
</style>
