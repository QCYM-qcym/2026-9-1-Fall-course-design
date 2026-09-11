<script setup>
import { MONTHLY_RANGE, DEMO_RANGE } from '../../utils/weatherWorkbench.js'
defineProps({ range: Array, disabled: Boolean })
defineEmits(['select'])
const presets = [{ label: '30 天月度数据', range: MONTHLY_RANGE }, { label: '旧兼容样例', range: DEMO_RANGE }]
</script>
<template>
  <div class="range-presets" aria-label="快捷时间范围">
    <button v-for="preset in presets" :key="preset.label" type="button" :disabled="disabled"
      :aria-pressed="range?.[0] === preset.range[0] && range?.[1] === preset.range[1]"
      @click="$emit('select', [...preset.range])">{{ preset.label }}</button>
  </div>
</template>
<style scoped>
.range-presets { display: flex; gap: 8px; flex-wrap: wrap; }
.range-presets button { min-height: 26px; padding: 3px 9px; border: 1px solid #466170; border-radius: 4px; background: transparent; color: #b6cad5; font-size: 11px; font-weight: 400; cursor: pointer; }
.range-presets button:hover:not(:disabled) { color: #eef5f7; border-color: #8eb8c7; background: #294657; }
.range-presets button[aria-pressed="true"] { color: #e4eecf; border-color: #829a84; background: #29444b; box-shadow: none; }
.range-presets button:focus-visible { outline: 2px solid #e4daa3; outline-offset: 3px; }
.range-presets button:disabled { opacity: .5; cursor: not-allowed; }
</style>
