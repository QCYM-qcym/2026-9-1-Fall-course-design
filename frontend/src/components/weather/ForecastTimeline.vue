<script setup>
defineProps({ times: Array, selectedTime: String, disabled: Boolean })
defineEmits(['select'])
</script>
<template>
  <footer class="forecast-timeline floating-panel" aria-label="预报时间轴">
    <div class="timeline-heading"><div><span class="micro-label">FORECAST TIMELINE</span><strong>{{ selectedTime || '等待预报时次' }}</strong></div><span class="timeline-note">{{ times.length }} 个时次 · 切换仅本地更新</span></div>
    <div class="timeline-track">
      <button v-for="time in times" :key="time" :disabled="disabled" :aria-pressed="time === selectedTime" :aria-label="`预报时次 ${time}`" @click="$emit('select', time)"><span>{{ time.slice(11, 16) }}</span><small>{{ time.slice(5, 10) }}</small></button>
      <span v-if="!times.length" class="muted">暂无可用预报时次</span>
    </div>
  </footer>
</template>
<style scoped>
.forecast-timeline { position: absolute; bottom: 23px; left: 24px; right: 24px; padding: 14px 20px; }
.timeline-heading { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 12px; }
.timeline-heading strong { margin-left: 22px; font-size: 12px; font-weight: 400; color: #d5e2e7; }
.timeline-note { color: #94afbc; font-size: 11px; }
.timeline-track { display: flex; gap: 8px; overflow-x: auto; min-height: 43px; }
.timeline-track button { min-width: 120px; flex: 1; display: flex; align-items: center; justify-content: center; gap: 14px; padding: 9px 14px; }
.timeline-track small { font-size: 10px; opacity: .65; }
@media(max-width: 1000px) { .forecast-timeline { left: 12px; right: 12px; bottom: 18px; } }
</style>
