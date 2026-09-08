import http from './http.js'
export async function fetchWeatherElements() {
  const { data } = await http.get('/weather-elements')
  if (data.code !== 200 || !Array.isArray(data.data)) throw new Error('Invalid element response')
  return data.data
}
