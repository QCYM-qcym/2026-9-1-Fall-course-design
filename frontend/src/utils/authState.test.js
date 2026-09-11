import test from 'node:test'
import assert from 'node:assert/strict'
import { createAuthState } from './authState.js'
import { createAuthGuard } from './authGuard.js'

const user = { id: 11, username: 'demo_user', role: 'USER' }
const admin = { id: 12, username: 'demo_admin', role: 'ADMIN' }
const denied = status => ({ response: { status } })
const deferred = () => { let resolve; const promise = new Promise(r => { resolve = r }); return { promise, resolve } }
const make = api => createAuthState({ me: async () => user, login: async () => user, logout: async () => {}, ...api })

test('restores safe server identity and shares the initial request', async () => {
  let calls = 0
  const pending = deferred()
  const auth = make({ me: () => { calls++; return pending.promise } })
  const a = auth.restore(); const b = auth.restore()
  assert.equal(auth.state.loading, true)
  pending.resolve({ ...user, passwordHash: 'must-not-retain' })
  await Promise.all([a, b])
  assert.equal(calls, 1)
  assert.deepEqual({ ...auth.state.currentUser }, user)
  assert.equal(auth.state.loading, false)
})
test('anonymous me is normal 401; unavailable backend is not reported as successful restore', async () => {
  const anonymous = make({ me: async () => { throw denied(401) } })
  await anonymous.restore()
  assert.equal(anonymous.state.currentUser, null)
  assert.equal(anonymous.state.initialized, true)
  const offline = make({ me: async () => { throw new Error('network') } })
  await assert.rejects(offline.restore())
  assert.equal(offline.state.initialized, false)
  assert.match(offline.state.error, /连接/)
})
test('login forwards selected entrance but identity uses the server role and safe fields', async () => {
  let sent
  const auth = make({ login: async payload => { sent = payload; return admin } })
  await auth.login({ username: 'demo_admin', password: 'demo-only', loginType: 'ADMIN' })
  assert.deepEqual(sent, { username: 'demo_admin', password: 'demo-only', loginType: 'ADMIN' })
  assert.equal(auth.state.currentUser.role, 'ADMIN')
  assert.equal(JSON.stringify(auth.state).includes('demo-only'), false)
})
test('401 and 403 login failures remain unauthenticated with distinct safe messages', async () => {
  for (const status of [401, 403]) {
    const auth = make({ login: async () => { throw denied(status) } })
    await assert.rejects(auth.login({}))
    assert.equal(auth.state.currentUser, null)
    assert.match(auth.state.error, status === 401 ? /账号或密码/ : /入口|权限/)
  }
})
test('logout suspends protected data immediately then clears identity after server success', async () => {
  const pending = deferred()
  const auth = make({ logout: () => pending.promise })
  await auth.restore()
  const epoch = auth.state.epoch
  const request = auth.logout()
  assert.equal(auth.state.suspended, true)
  assert.ok(auth.state.epoch > epoch)
  pending.resolve()
  await request
  assert.equal(auth.state.currentUser, null)
  assert.equal(auth.state.suspended, false)
})
test('failed logout is visible and never reported as server invalidation', async () => {
  const auth = make({ logout: async () => { throw new Error('offline') } })
  await auth.restore()
  await assert.rejects(auth.logout())
  assert.equal(auth.state.currentUser.username, user.username)
  assert.match(auth.state.error, /退出失败/)
  assert.equal(auth.state.suspended, false)
})
test('expiry invalidates stale restore/login results, including after a newer login', async () => {
  const pending = deferred()
  const auth = make({ me: () => pending.promise })
  const request = auth.restore()
  auth.expire()
  await auth.login({})
  pending.resolve(admin)
  await request
  assert.equal(auth.state.currentUser.role, 'USER')
})
test('old login cannot restore identity after logout', async () => {
  const pending = deferred()
  const auth = make({ login: () => pending.promise })
  const login = auth.login({})
  await auth.logout()
  pending.resolve(admin)
  await login
  assert.equal(auth.state.currentUser, null)
})
test('guards require authentication and reject USER management without escalating role', async () => {
  const auth = make()
  const guard = createAuthGuard(auth)
  assert.equal(await guard({ path: '/weather' }), true)
  assert.equal(await guard({ path: '/analysis' }), true)
  assert.equal(await guard({ path: '/comparison' }), true)
  assert.equal(await guard({ path: '/management', meta: { requiresAdmin: true } }), '/weather')
  assert.match(auth.state.notice, /权限/)
  auth.expire()
  assert.equal(await guard({ path: '/weather' }), '/login')
})
test('ADMIN management allowed; both roles land on weather when already logged in', async () => {
  const auth = make({ me: async () => admin })
  const guard = createAuthGuard(auth)
  assert.equal(await guard({ path: '/management', meta: { requiresAdmin: true } }), true)
  assert.equal(await guard({ path: '/login', name: 'login' }), '/weather')
})
test('anonymous login remains accessible when me fails', async () => {
  const auth = make({ me: async () => { throw new Error('offline') } })
  const guard = createAuthGuard(auth)
  assert.equal(await guard({ path: '/login', name: 'login' }), true)
  assert.equal(await guard({ path: '/comparison' }), '/login')
})
