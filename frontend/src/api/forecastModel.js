import http from './http.js'
export async function fetchForecastModels() {
  const { data } = await http.get('/forecast-models')
  if (data.code !== 200 || !Array.isArray(data.data)) throw new Error('Invalid model response')
  return data.data
}
