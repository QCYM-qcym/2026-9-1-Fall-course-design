import { reactive } from 'vue'
import { DEMO_RANGE, selectDefault, resolveGeoName } from './weatherWorkbench.js'
import { buildTrendQuery, isTrendResponse } from './trendAnalysis.js'

export function createTrendState(api) {
  let requestVersion = 0
  let disposed = false
  const state = reactive({ cities: [], models: [], cityId: null, modelId: null, range: [...DEMO_RANGE],
    dictionariesReady: false, status: 'idle', error: '', warning: '', response: null, appliedQuery: null })

  async function initialize() {
    if (disposed) return
    const version = ++requestVersion
    state.status = 'loading'; state.error = ''; state.response = null
    try {
      const [allCities, allModels] = await Promise.all([api.fetchCities(), api.fetchForecastModels()])
      if (disposed || version !== requestVersion) return
      const cities = allCities.filter(city => resolveGeoName(city.cityCode))
      const models = allModels.filter(model => ['ECMWF', 'NOAA'].includes(model.modelCode))
      const city = cities.find(item => item.id === state.cityId) ?? selectDefault(cities, 'cityCode', 'JINAN')
      const model = models.find(item => item.id === state.modelId) ?? selectDefault(models, 'modelCode', 'ECMWF')
      if (!city || !model) throw new Error('Missing supported dictionaries')
      Object.assign(state, { cities, models, cityId: city.id, modelId: model.id, dictionariesReady: true })
      state.warning = !cities.some(item => item.cityCode === 'JINAN') || !models.some(item => item.modelCode === 'ECMWF')
        ? '默认演示编码缺失，已选择字典中可用的核心城市或模型。' : ''
      await load()
    } catch {
      if (disposed || version !== requestVersion) return
      state.status = 'error'
      state.error = '城市或模型字典加载失败，或没有可用的核心选项。请确认后端服务后重试。'
    }
  }

  async function load() {
    if (disposed) return
    const version = ++requestVersion
    state.response = null; state.error = ''; state.appliedQuery = null
    let params
    try { params = buildTrendQuery(state) }
    catch (error) { state.status = 'error'; state.error = error.message; return }
    if (!state.dictionariesReady) { state.status = 'error'; state.error = '请先加载城市与模型字典。'; return }
    state.status = 'loading'
    try {
      const data = await api.fetchTrend(params)
      if (disposed || version !== requestVersion) return
      if (!isTrendResponse(data) || data.cityId !== params.cityId || data.modelId !== params.modelId) throw new Error('Invalid trend response')
      state.response = data; state.appliedQuery = params
      state.status = data.temperature.length || data.precipitation.length ? 'success' : 'empty'
    } catch {
      if (disposed || version !== requestVersion) return
      state.status = 'error'
      state.error = '趋势数据加载失败，请确认后端服务与查询条件，然后重试。'
    }
  }

  function retry() { return state.dictionariesReady ? load() : initialize() }
  function dispose() { disposed = true; requestVersion++ }
  return { state, initialize, load, retry, dispose }
}
