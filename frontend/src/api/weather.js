import http from './http.js'
import { isTrendResponse } from '../utils/trendAnalysis.js'
export async function fetchWorkbench(params) {
  const { data } = await http.get('/weather/workbench', { params })
  if (data.code !== 200) throw new Error('Invalid workbench response')
  return data.data
}

export async function fetchTrend({ cityId, modelId, startTime, endTime }) {
  const { data } = await http.get('/weather/trend', { params: { cityId, modelId, startTime, endTime } })
  if (data.code !== 200 || !isTrendResponse(data.data)) throw new Error('Invalid trend response')
  return data.data
}
