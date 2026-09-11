<script setup>
import { computed } from 'vue'
import { groupForecastTimes, selectDateTime } from '../../utils/weatherWorkbench.js'
const props = defineProps({ times: { type: Array, default: () => [] }, selectedTime: String, disabled: Boolean })
const emit = defineEmits(['select'])
const days = computed(() => groupForecastTimes(props.times))
const selectedDate = computed(() => props.selectedTime?.slice(0, 10) ?? '')
const slots = computed(() => days.value.find(day => day.date === selectedDate.value)?.times ?? [])
function changeDate(date) {
  if (!props.disabled) emit('select', selectDateTime(props.times, date, props.selectedTime))
}
</script>
<template>
  <footer class="forecast-timeline floating-panel" aria-label="预报时间轴">
    <div class="timeline-heading">
      <label class="date-control">预报日期
        <select aria-label="预报日期" :value="selectedDate" :disabled="disabled || !days.length" @change="changeDate($event.target.value)">
          <option v-if="!days.length" value="">暂无日期</option>
          <option v-for="day in days" :key="day.date" :value="day.date">{{ day.date }}</option>
        </select>
      </label>
      <strong aria-live="polite">{{ selectedTime || '等待预报时次' }}</strong>
      <span class="timeline-note">{{ days.length }} 天 / {{ times.length }} 时次 · 当日 {{ slots.length }} 时次 · 本地业务时间</span>
    </div>
    <div class="timeline-track">
      <button v-for="time in slots" :key="time" :disabled="disabled" :aria-pressed="time === selectedTime" :aria-label="`预报时次 ${time}`" @click="$emit('select', time)">{{ time.slice(11, 16) }}</button>
      <span v-if="!slots.length" class="muted">暂无可用预报时次</span>
    </div>
  </footer>
</template>
<style scoped>
.forecast-timeline { position: absolute; bottom: 23px; left: 24px; right: 24px; padding: 12px 20px; }
.timeline-heading { display: flex; align-items: center; gap: 20px; margin-bottom: 10px; }
.date-control { display: flex; align-items: center; gap: 10px; font-size: 12px; color: #b8ced8; }
select { height: 32px; padding: 0 10px; border: 1px solid #52707e; border-radius: 4px; background: #203744; color: #e5eef2; font: inherit; color-scheme: dark; }
.timeline-heading strong { font-size: 13px; font-weight: 500; color: #e4eecf; font-variant-numeric: tabular-nums; }
.timeline-note { margin-left: auto; color: #a7bfcb; font-size: 11px; }
.timeline-track { display: grid; grid-template-columns: repeat(8, minmax(0, 1fr)); gap: 8px; min-height: 36px; }
.timeline-track button { min-width: 0; padding: 9px 4px; font-variant-numeric: tabular-nums; }
@media(max-width: 1000px) { .forecast-timeline { left: 12px; right: 12px; bottom: 18px; }.timeline-heading { flex-wrap: wrap; gap: 8px 16px; }.timeline-note { margin-left: 0; } }
</style>
