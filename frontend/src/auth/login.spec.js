import { afterEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import Login from '../views/Login.vue'
import App from '../App.vue'
import { auth } from './index.js'
import { createAuthGuard } from '../utils/authGuard.js'

const roots = []
afterEach(() => { roots.forEach(wrapper => wrapper.unmount()); roots.length = 0; vi.restoreAllMocks(); auth.expire() })
const user = { id: 1, username: 'demo_user', role: 'USER' }
const admin = { id: 2, username: 'demo_admin', role: 'ADMIN' }
async function mountPage(identity = null) {
  Object.assign(auth.state, { currentUser: identity, initialized: true, error: '', notice: '', loading: false })
  const router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/login', name: 'login', component: Login },
    ...['weather', 'analysis', 'comparison', 'management'].map(path => ({ path: '/' + path, meta: { requiresAdmin: path === 'management' }, component: { template: `<div>${path} protected</div>` } }))
  ] })
  router.beforeEach(createAuthGuard(auth))
  await router.push(identity ? '/weather' : '/login')
  await router.isReady()
  const wrapper = mount(App, { global: { plugins: [router] } })
  roots.push(wrapper)
  return { wrapper, router }
}
describe('login and protected shell', () => {
  it('offers two entrances, submits selected credentials and lands on weather', async () => {
    const login = vi.spyOn(auth, 'login').mockImplementation(async credentials => { auth.state.currentUser = { ...admin }; expect(credentials.loginType).toBe('ADMIN') })
    const { wrapper, router } = await mountPage()
    await wrapper.get('[data-login-type="ADMIN"]').trigger('click')
    await wrapper.get('input[name="username"]').setValue('demo_admin')
    await wrapper.get('input[name="password"]').setValue('DemoAdmin@2026')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(login).toHaveBeenCalledWith({ username: 'demo_admin', password: 'DemoAdmin@2026', loginType: 'ADMIN' })
    expect(router.currentRoute.value.path).toBe('/weather')
    expect(wrapper.text()).toContain('demo_admin')
  })
  it('failed login displays a safe message and clears the password field', async () => {
    vi.spyOn(auth, 'login').mockImplementation(async () => { auth.state.error = '账号或密码错误'; throw new Error('sensitive SQL') })
    const { wrapper, router } = await mountPage()
    await wrapper.get('input[name="username"]').setValue('unknown')
    await wrapper.get('input[name="password"]').setValue('incorrect')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/login')
    expect(wrapper.text()).toContain('账号或密码错误')
    expect(wrapper.text()).not.toContain('sensitive SQL')
    expect(wrapper.get('input[name="password"]').element.value).toBe('')
  })
  it('USER menu hides management and direct navigation is blocked; ADMIN sees it', async () => {
    const { wrapper, router } = await mountPage(user)
    expect(wrapper.find('a[href="/management"]').exists()).toBe(false)
    await router.push('/management')
    expect(router.currentRoute.value.path).toBe('/weather')
    expect(wrapper.text()).toContain('没有数据管理权限')
    auth.state.currentUser = admin
    await flushPromises()
    expect(wrapper.find('a[href="/management"]').exists()).toBe(true)
  })
  it('management trailing slash and case variants cannot bypass USER route permissions', async () => {
    const { router, wrapper } = await mountPage(user)
    for (const path of ['/management/', '/Management']) {
      await router.push(path)
      expect(router.currentRoute.value.path).toBe('/weather')
      expect(wrapper.text()).not.toContain('management protected')
    }
  })
  it('expired session removes protected content and cannot be restored by browser back', async () => {
    const { wrapper, router } = await mountPage(admin)
    await router.push('/management')
    auth.expire()
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/login')
    expect(wrapper.text()).not.toContain('management protected')
    router.back()
    await flushPromises()
    expect(wrapper.text()).not.toContain('protected')
  })
  it('shows pending login with submit disabled and preserves the form shell', async () => {
    const { wrapper } = await mountPage()
    auth.state.loading = true
    await flushPromises()
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('登录中')
    expect(wrapper.find('input[name="username"]').exists()).toBe(true)
  })
})
