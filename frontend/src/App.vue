<script setup>
import { watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { auth } from './auth/index.js'
const route = useRoute()
const router = useRouter()
watch(() => auth.state.currentUser, user => {
  if (!user && auth.state.initialized && route.name !== 'login') router.replace('/login')
})
async function logout() {
  try { await auth.logout(); if (!auth.state.currentUser) await router.replace('/login') }
  catch { /* Do not claim the server session ended when the request failed. */ }
}
</script>

<template>
  <div class="app-shell" :class="{ 'weather-shell': route.path === '/weather' }">
    <header class="app-header">
      <div>
        <p class="brand-kicker">SHANDONG WEATHER</p>
        <strong>山东省气象预报数据可视化系统</strong>
      </div>
      <nav v-if="auth.state.currentUser" aria-label="主导航">
        <RouterLink to="/weather">气象工作台</RouterLink>
        <RouterLink to="/analysis">趋势分析</RouterLink>
        <RouterLink to="/comparison">模型对比</RouterLink>
        <RouterLink v-if="auth.state.currentUser.role === 'ADMIN'" to="/management">数据管理</RouterLink>
      </nav>
      <div v-if="auth.state.currentUser" class="auth-identity">
        <span>{{ auth.state.currentUser.username }} · {{ auth.state.currentUser.role }}</span>
        <button type="button" :disabled="auth.state.loading" @click="logout">退出登录</button>
      </div>
    </header>
    <main class="app-main">
      <p v-if="auth.state.notice" role="status" class="auth-notice">{{ auth.state.notice }}</p>
      <p v-if="auth.state.error && route.name !== 'login'" role="alert" class="auth-notice">{{ auth.state.error }}</p>
      <RouterView v-if="route.name === 'login' || (auth.state.currentUser && !auth.state.suspended)" :key="auth.state.epoch" />
    </main>
  </div>
</template>

<style>
.auth-identity { display: flex; align-items: center; gap: 12px; font-size: 12px; color: #b9cce0; }
.auth-identity button { color: #dbeafe; background: #1e3b5b; border: 1px solid #395777; border-radius: 5px; padding: 6px 10px; cursor: pointer; }
.auth-notice { padding: 12px 20px; color: #ffd9a3; background: #273548; }
.weather-shell .app-main { width: 100%; padding: 0; margin: 0; }
.weather-shell .app-header { height: 92px; padding: 14px 25px; }
@media(max-width: 760px) { .weather-shell .app-header { height: auto; } }
</style>
