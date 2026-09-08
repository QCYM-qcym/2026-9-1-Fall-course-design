<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>

<template>
  <div class="app-shell" :class="{ 'weather-shell': route.path === '/weather' }">
    <header class="app-header">
      <div>
        <p class="brand-kicker">SHANDONG WEATHER</p>
        <strong>山东省气象预报数据可视化系统</strong>
      </div>
      <nav aria-label="主导航">
        <RouterLink to="/weather">气象工作台</RouterLink>
        <RouterLink to="/analysis">趋势分析</RouterLink>
        <RouterLink to="/comparison">模型对比</RouterLink>
        <RouterLink to="/management">数据管理</RouterLink>
      </nav>
    </header>
    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>

<style>
.weather-shell .app-main { width: 100%; padding: 0; margin: 0; }
.weather-shell .app-header { height: 92px; padding: 14px 25px; }
@media(max-width: 760px) { .weather-shell .app-header { height: auto; } }
</style>
