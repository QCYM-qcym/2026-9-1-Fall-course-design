<script setup>
defineProps({ cities: Array, models: Array, cityId: Number, modelId: Number, range: Array, ready: Boolean, loading: Boolean })
defineEmits(['city', 'model', 'range', 'query'])
</script>

<template>
  <form class="trend-filter-bar" aria-label="趋势查询条件" @submit.prevent="$emit('query')">
    <label class="trend-field"><span>城市 / CITY</span>
      <select aria-label="趋势城市" :value="cityId ?? ''" :disabled="!ready" @change="$emit('city', Number($event.target.value))">
        <option v-if="!cities.length" value="" disabled>等待城市字典</option>
        <option v-for="city in cities" :key="city.id" :value="city.id">{{ city.cityName }}</option>
      </select>
    </label>
    <label class="trend-field"><span>预报模型 / MODEL</span>
      <select aria-label="趋势模型" :value="modelId ?? ''" :disabled="!ready" @change="$emit('model', Number($event.target.value))">
        <option v-if="!models.length" value="" disabled>等待模型字典</option>
        <option v-for="model in models" :key="model.id" :value="model.id">{{ model.modelName }}</option>
      </select>
    </label>
    <div class="trend-field trend-range"><span id="trend-range-label">时间范围 / 本地业务时间</span>
      <el-date-picker :model-value="range" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm"
        start-placeholder="开始时间" end-placeholder="结束时间" range-separator="—" :clearable="false" :disabled="!ready"
        aria-labelledby="trend-range-label" @update:model-value="$emit('range', $event)" />
    </div>
    <button type="submit" :disabled="!ready">{{ loading && ready ? '重新查询' : '查询趋势' }} <span aria-hidden="true">↗</span></button>
  </form>
</template>

<style scoped>
.trend-filter-bar { display: grid; grid-template-columns: minmax(130px, 1fr) minmax(150px, 1fr) minmax(370px, 2.7fr) auto; align-items: end; gap: 18px; }
.trend-field { display: grid; gap: 9px; min-width: 0; }
.trend-field > span { font-size: 10px; letter-spacing: .07em; color: #9db5c3; }
select { width: 100%; min-width: 0; height: 38px; padding: 0 12px; border: 1px solid #466170; border-radius: 6px; background: #203846; color: #e3edf0; font: inherit; font-size: 13px; }
.trend-range :deep(.el-date-editor) { width: 100%; height: 38px; box-sizing: border-box; }
.trend-range :deep(.el-range-input) { min-width: 0; font-size: 12px; }
button { min-height: 38px; display: flex; align-items: center; gap: 24px; padding: 0 20px; color: #14282d; background: #d1dba7; border: 1px solid #d1dba7; border-radius: 6px; cursor: pointer; font-size: 13px; font-weight: 600; }
button:hover:not(:disabled) { background: #e0e8c3; } button:disabled, select:disabled { opacity: .5; cursor: not-allowed; }
button:focus-visible, select:focus-visible { outline: 2px solid #e7dfa2; outline-offset: 3px; }
@media(max-width: 1000px) { .trend-filter-bar { grid-template-columns: 1fr 1fr; gap: 14px; }.trend-range { grid-column: 1 / -1; } button { grid-column: 1 / -1; justify-content: center; } }
</style>
