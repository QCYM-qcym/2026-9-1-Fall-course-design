import http from './http.js'
export async function fetchCities() {
  const { data } = await http.get('/cities')
  if (data.code !== 200 || !Array.isArray(data.data)) throw new Error('Invalid city response')
  return data.data
}
