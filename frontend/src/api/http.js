import axios, { CanceledError } from 'axios'

export const http = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true
})

let authTransport = {}
export function configureAuthTransport(transport) { authTransport = transport }
const epoch = () => authTransport.epoch?.() ?? 0

http.interceptors.request.use(async config => {
  config.authEpoch = epoch()
  if (!['get', 'head', 'options'].includes(config.method)) {
    // Refresh after login/logout rotation. Never replay a failed write automatically.
    const response = await http.get('/auth/csrf', { ignoreAuthFailure: true })
    if (config.authEpoch !== epoch()) throw new CanceledError('Session changed')
    const csrf = response.data?.data
    if (response.data?.code !== 200 || !csrf?.token || csrf.headerName !== 'X-XSRF-TOKEN') throw new Error('CSRF unavailable')
    config.headers.set(csrf.headerName, csrf.token)
  }
  return config
})
http.interceptors.response.use(response => response, error => {
  if (error.response?.status === 401 && !error.config?.ignoreAuthFailure && error.config?.authEpoch === epoch()) authTransport.expire?.()
  return Promise.reject(error)
})

export default http
