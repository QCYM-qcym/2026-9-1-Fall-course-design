import http from './http.js'
export async function fetchWorkbench(params) {
  const { data } = await http.get('/weather/workbench', { params })
  if (data.code !== 200) throw new Error('Invalid workbench response')
  return data.data
}
