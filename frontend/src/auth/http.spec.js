import { afterEach, describe, expect, it } from 'vitest'
import { AxiosError } from 'axios'
import http, { configureAuthTransport } from '../api/http.js'

const original = http.defaults.adapter
afterEach(() => { http.defaults.adapter = original; configureAuthTransport({}) })
const response = (config, data) => ({ config, status: 200, statusText: 'OK', headers: {}, data: { code: 200, data } })
describe('same-origin authentication transport', () => {
  it('fetches fresh CSRF and adds its header for each write without a second Axios instance', async () => {
    const requests = []
    let token = 0
    http.defaults.adapter = async config => {
      requests.push(config)
      return response(config, config.url === '/auth/csrf' ? { token: 'csrf-' + ++token, headerName: 'X-XSRF-TOKEN', parameterName: '_csrf' } : 'ok')
    }
    await http.post('/cities', {})
    await http.delete('/cities/99')
    expect(requests.map(r => r.url)).toEqual(['/auth/csrf', '/cities', '/auth/csrf', '/cities/99'])
    expect(requests[1].headers.get('X-XSRF-TOKEN')).toBe('csrf-1')
    expect(requests[3].headers.get('X-XSRF-TOKEN')).toBe('csrf-2')
    expect(requests[1].withCredentials).toBe(true)
  })
  it('ordinary GET needs no CSRF request and 403 does not clear the current session', async () => {
    const calls = []
    let expired = false
    configureAuthTransport({ expire: () => { expired = true } })
    http.defaults.adapter = async config => {
      calls.push(config.url)
      throw new AxiosError('Forbidden', 'ERR_BAD_REQUEST', config, null, { status: 403, data: { code: 403 }, config })
    }
    await expect(http.get('/forecast-records')).rejects.toBeTruthy()
    expect(calls).toEqual(['/forecast-records'])
    expect(expired).toBe(false)
  })
  it('a current business 401 expires the session but an old generation 401 cannot log out a new user', async () => {
    let epoch = 1; let expires = 0; let finish
    configureAuthTransport({ epoch: () => epoch, expire: () => { expires++ } })
    http.defaults.adapter = config => new Promise((resolve, reject) => { finish = () => reject(new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, null, { status: 401, config })) })
    const first = http.get('/cities')
    await new Promise(resolve => setTimeout(resolve, 0))
    epoch = 2; finish()
    await expect(first).rejects.toBeTruthy()
    expect(expires).toBe(0)
    const next = http.get('/cities')
    await new Promise(resolve => setTimeout(resolve, 0))
    finish()
    await expect(next).rejects.toBeTruthy()
    expect(expires).toBe(1)
  })
  it('cannot send a queued write after logout changes the session generation', async () => {
    let epoch = 1; let finish; const urls = []
    configureAuthTransport({ epoch: () => epoch })
    http.defaults.adapter = config => {
      urls.push(config.url)
      return new Promise(resolve => { finish = () => resolve(response(config, { token: 't', headerName: 'X-XSRF-TOKEN' })) })
    }
    const request = http.put('/cities/99', {})
    await new Promise(resolve => setTimeout(resolve, 0))
    epoch++; finish()
    await expect(request).rejects.toMatchObject({ code: 'ERR_CANCELED' })
    expect(urls).toEqual(['/auth/csrf'])
  })
})
