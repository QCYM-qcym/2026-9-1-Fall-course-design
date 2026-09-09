import http from './http.js'
import { isTrendResponse } from '../utils/trendAnalysis.js'
import { isComparisonResponse } from '../utils/comparisonAnalysis.js'
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

export async function fetchComparison({ cityId, elementId, startTime, endTime }) {
  const { data } = await http.get('/weather/comparison', { params: { cityId, elementId, startTime, endTime } })
  if (data?.code !== 200 || !isComparisonResponse(data.data)) throw new Error('Invalid comparison response')
  return data.data
}
