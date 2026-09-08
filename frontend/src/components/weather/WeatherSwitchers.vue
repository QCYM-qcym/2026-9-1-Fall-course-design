<script setup>
defineProps({ models: Array, elements: Array, modelId: Number, elementId: Number, disabled: Boolean })
defineEmits(['model', 'element'])
</script>
<template>
  <aside class="weather-switchers floating-panel">
    <p class="micro-label">预报模型</p>
    <div class="switch-group" aria-label="预报模型">
      <button v-for="model in models" :key="model.id" :aria-pressed="model.id === modelId" :disabled="disabled" @click="$emit('model', model.id)">{{ model.modelName }}</button>
      <span v-if="!models.length" class="muted">等待字典</span>
    </div>
    <div class="switch-divider" />
    <p class="micro-label">气象图层</p>
    <div class="switch-group" aria-label="气象要素">
      <button v-for="element in elements" :key="element.id" :aria-pressed="element.id === elementId" :disabled="disabled" @click="$emit('element', element.id)"><span>{{ element.elementCode }}</span><small>{{ element.elementName }}</small></button>
      <span v-if="!elements.length" class="muted">等待字典</span>
    </div>
  </aside>
</template>
<style scoped>
.weather-switchers { position: absolute; top: 152px; left: 24px; width: 145px; padding: 17px 13px; }
.switch-group { display: grid; gap: 7px; margin-top: 10px; }
.switch-group button { text-align: left; padding: 11px 12px; }
.switch-group small { display: block; font-size: 11px; margin-top: 5px; opacity: .8; }
.switch-divider { height: 1px; background: #34505d; margin: 20px 0; }
@media(max-width: 1000px) { .weather-switchers { top: auto; bottom: 146px; left: 12px; width: 240px; padding: 12px; } .switch-group { display: flex; } .switch-divider { margin: 8px 0; } .switch-group button { padding: 5px 10px; } }
</style>
