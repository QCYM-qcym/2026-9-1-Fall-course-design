<script setup>
defineProps({ model: Object, element: Object, range: Array, disabled: Boolean })
defineEmits(['range'])
</script>
<template>
  <header class="weather-toolbar floating-panel">
    <div class="toolbar-title">
      <p class="micro-label">WEATHER WORKBENCH <span class="demo-badge">课程演示 · 合成数据</span></p>
      <h1>山东省气象预报工作台</h1>
      <p class="toolbar-subtitle">{{ model?.modelName ?? '模型待加载' }} <span> / </span> {{ element?.elementName ?? '要素待加载' }} <span> / </span> 山东 16 市</p>
    </div>
    <div class="range-control">
      <span class="micro-label">查询范围 · 本地业务时间</span>
      <el-date-picker :model-value="range" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss" format="MM-DD HH:mm" start-placeholder="开始时间" end-placeholder="结束时间" range-separator="—" :clearable="false" :disabled="disabled" @update:model-value="$emit('range', $event)" />
    </div>
  </header>
</template>
<style scoped>
.weather-toolbar { position: absolute; top: 20px; left: 24px; right: 24px; display: flex; justify-content: space-between; align-items: center; gap: 24px; padding: 16px 22px; }
h1 { margin: 6px 0; font-size: 22px; font-weight: 600; letter-spacing: .04em; }
.toolbar-subtitle { margin: 0; color: #b0c4ce; font-size: 12px; }
.toolbar-subtitle span { color: #658190; padding: 0 8px; }
.demo-badge { font-size: 10px; color: #d4cba7; border: 1px solid #6b6240; padding: 3px 6px; border-radius: 4px; margin-left: 12px; letter-spacing: 0; }
.range-control { display: grid; gap: 8px; }
.range-control :deep(.el-date-editor) { width: 370px; }
@media(max-width: 1000px) { .weather-toolbar { gap: 10px; padding: 12px; left: 12px; right: 12px; } h1 { font-size: 18px; } .demo-badge { display: block; width: fit-content; margin: 5px 0 0; } .range-control :deep(.el-date-editor) { width: 280px; } }
@media(max-width: 650px) { .weather-toolbar { flex-wrap: wrap; } }
</style>
