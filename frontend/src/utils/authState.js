import { reactive } from 'vue'

// Each session generation owns its views/cache. Never retain credentials in state.
export function createAuthState(api) {
  const state = reactive({ currentUser: null, initialized: false, loading: false, suspended: false, error: '', notice: '', epoch: 0 })
  let version = 0
  let restoring = null
  const safeUser = value => {
    if (!value?.id || typeof value.username !== 'string' || !['USER', 'ADMIN'].includes(value.role)) throw new Error('Invalid identity')
    return { id: value.id, username: value.username, role: value.role }
  }
  function expire() {
    version++
    state.epoch++
    Object.assign(state, { currentUser: null, initialized: true, loading: false, suspended: false, notice: '登录已失效，请重新登录。' })
    restoring = null
  }
  async function restore() {
    if (state.initialized) return
    if (restoring) return restoring
    const current = ++version
    state.loading = true
    const pending = (async () => {
      try {
        const identity = await api.me()
        if (current !== version) return
        state.currentUser = safeUser(identity)
        state.initialized = true
      } catch (error) {
        if (current !== version) return
        state.currentUser = null
        if (error.response?.status === 401) state.initialized = true
        else { state.error = '无法连接认证服务，请检查后端后重试。'; throw error }
      } finally {
        if (current === version) { state.loading = false; restoring = null }
      }
    })()
    restoring = pending
    return pending
  }
  async function login(credentials) {
    const current = ++version
    state.epoch++
    Object.assign(state, { currentUser: null, loading: true, error: '', notice: '' })
    try {
      const identity = await api.login(credentials)
      if (current !== version) return
      state.currentUser = safeUser(identity)
      state.initialized = true
    } catch (error) {
      if (current !== version) return
      const status = error.response?.status
      state.error = status === 401 ? '账号或密码错误，或账号已停用。' : status === 403 ? '登录入口与账号角色不符，或安全校验失败，请重试。' : '登录失败，请检查网络或后端服务。'
      throw error
    } finally { if (current === version) state.loading = false }
  }
  async function logout() {
    const current = ++version
    state.epoch++
    Object.assign(state, { loading: true, suspended: true, error: '' })
    try {
      await api.logout()
      if (current === version) {
        state.currentUser = null
        state.initialized = true
        state.notice = ''
      }
    } catch (error) {
      if (current !== version) return
      if (error.response?.status === 401) state.currentUser = null
      else { state.error = '退出失败，服务端会话尚未确认失效，请重试。'; throw error }
    } finally { if (current === version) { state.loading = false; state.suspended = false } }
  }
  return { state, restore, login, logout, expire }
}
