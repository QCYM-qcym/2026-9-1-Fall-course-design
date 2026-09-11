import http from './http.js'

async function request(method, url, data) {
  const response = await http.request({ method, url, data, ignoreAuthFailure: true })
  if (response.data?.code !== 200) throw { response: { status: response.data?.code ?? 500 } }
  return response.data.data
}
export const me = () => request('get', '/auth/me')
export const login = credentials => request('post', '/auth/login', credentials)
export const logout = () => request('post', '/auth/logout')
