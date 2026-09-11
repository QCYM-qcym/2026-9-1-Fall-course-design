<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { auth } from '../auth/index.js'
const router = useRouter()
const loginType = ref('USER')
const username = ref('')
const password = ref('')
async function submit() {
  if (auth.state.loading || !username.value.trim() || !password.value) return
  try {
    await auth.login({ username: username.value.trim(), password: password.value, loginType: loginType.value })
    if (auth.state.currentUser) await router.replace('/weather')
  } catch { /* Only the auth state's safe message is displayed. */ }
  finally { password.value = '' }
}
</script>

<template>
  <section class="login-card" aria-label="身份登录">
    <p class="eyebrow">SHANDONG WEATHER / SIGN IN</p>
    <h1>山东省气象预报数据可视化系统</h1>
    <p class="login-caption">课程合成数据演示 · 请选择与账号相符的登录入口</p>
    <div class="login-modes" aria-label="登录方式">
      <button v-for="mode in ['USER', 'ADMIN']" :key="mode" type="button" :data-login-type="mode" :aria-pressed="loginType === mode" :disabled="auth.state.loading" @click="loginType = mode">
        {{ mode === 'USER' ? '普通用户登录' : '管理员登录' }}
      </button>
    </div>
    <form @submit.prevent="submit">
      <label>用户名<input v-model="username" name="username" autocomplete="username" maxlength="64" required :disabled="auth.state.loading" /></label>
      <label>密码<input v-model="password" name="password" type="password" autocomplete="current-password" maxlength="128" required :disabled="auth.state.loading" /></label>
      <p v-if="auth.state.error" role="alert" class="login-error">{{ auth.state.error }}</p>
      <button class="login-submit" type="submit" :disabled="auth.state.loading">{{ auth.state.loading ? '登录中…' : '登录' }}</button>
    </form>
    <p class="login-caption">角色由服务器验证；选择管理员入口不会提升账号权限。</p>
  </section>
</template>

<style scoped>
.login-card { max-width: 520px; margin: 2vh auto; padding: 32px; border: 1px solid #2c4664; border-radius: 12px; background: #14243a; }
h1 { font-size: 24px; line-height: 1.5; }
.login-caption { color: #a8b8ca; font-size: 13px; line-height: 1.7; }
.login-modes { display: flex; gap: 8px; margin: 24px 0; }
button { cursor: pointer; color: #dbeafe; border: 1px solid #395777; border-radius: 6px; background: #19314c; padding: 11px; }
.login-modes button { flex: 1; }
.login-modes button[aria-pressed="true"] { background: #24649a; border-color: #78bdff; }
label { display: grid; gap: 8px; margin: 18px 0; color: #bdcfe2; }
input { width: 100%; padding: 12px; border: 1px solid #395777; border-radius: 6px; background: #0d1b2d; color: white; font: inherit; }
input:focus-visible, button:focus-visible { outline: 2px solid #70b7ff; outline-offset: 2px; }
.login-submit { width: 100%; background: #235f96; margin-top: 8px; }
button:disabled { cursor: wait; opacity: .65; }
.login-error { color: #ffb4ab; }
</style>
