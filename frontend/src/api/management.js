import http from './http.js'
export { fetchCities } from './city.js'
export { fetchForecastModels } from './forecastModel.js'
export { fetchWeatherElements } from './weatherElement.js'

async function request(method,url,body) {
  const response=await http.request({method,url,...(body === undefined ? {} : {data:body})})
  if(response.data?.code !== 200) throw {response:{status:response.data?.code || 500,data:response.data}}
  return response.data.data
}
export async function fetchForecastRecords() {
  const rows=await request('get','/forecast-records')
  if(!Array.isArray(rows)) throw {response:{status:500}}
  return rows
}
export const createCity = body => request('post','/cities',body)
export const updateCity = (id,body) => request('put','/cities/'+id,body)
export const deleteCity = id => request('delete','/cities/'+id)
export const createForecastModel = body => request('post','/forecast-models',body)
export const updateForecastModel = (id,body) => request('put','/forecast-models/'+id,body)
export const deleteForecastModel = id => request('delete','/forecast-models/'+id)
export const createWeatherElement = body => request('post','/weather-elements',body)
export const updateWeatherElement = (id,body) => request('put','/weather-elements/'+id,body)
export const deleteWeatherElement = id => request('delete','/weather-elements/'+id)
export const createForecastRecord = body => request('post','/forecast-records',body)
export const updateForecastRecord = (id,body) => request('put','/forecast-records/'+id,body)
export const deleteForecastRecord = id => request('delete','/forecast-records/'+id)
