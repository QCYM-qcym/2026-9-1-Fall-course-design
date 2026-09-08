import { reactive } from 'vue'
import { DEMO_RANGE, buildWorkbenchCacheKey, createWorkbenchCache, selectDefault, validateCityMapping } from './weatherWorkbench.js'

// Dependencies are the real API module in the page; tests substitute only the network boundary.
export function createWorkbenchState(api, geo) {
  const cache = createWorkbenchCache()
  let requestVersion = 0
  let disposed = false
  const state = reactive({
    cities: [], models: [], elements: [], modelId: null, elementId: null, range: [...DEMO_RANGE],
    response: null, selectedTime: '', selectedCityId: null, status: 'idle', error: '', warning: '',
    dictionariesReady: false, matched: 0, source: ''
  })

  async function initialize() {
    const version = ++requestVersion
    state.status = 'loading'
    state.error = ''
    try {
      const [cities, models, elements] = await Promise.all([api.fetchCities(), api.fetchForecastModels(), api.fetchWeatherElements()])
      if (disposed || version !== requestVersion) return
      const mapping = validateCityMapping(cities, geo)
      const model = selectDefault(models, 'modelCode', 'ECMWF')
      const element = selectDefault(elements, 'elementCode', 'T2M')
      if (!model || !element) throw new Error('Empty dictionary')
      Object.assign(state, { cities, models, elements, modelId: model.id, elementId: element.id, matched: mapping.matched, dictionariesReady: true })
      state.warning = model.modelCode !== 'ECMWF' || element.elementCode !== 'T2M' ? '默认演示编码缺失，已使用字典中的首个可用选项。' : ''
      await load()
    } catch {
      if (disposed || version !== requestVersion) return
      state.status = 'error'
      state.error = '城市、模型或要素字典加载失败，或山东 16 市映射不完整。请确认后端服务及字典后重试。'
    }
  }

  async function load(selection = {}) {
    // Increment even on cache hits: an older request must never replace the latest choice.
    const version = ++requestVersion
    if (disposed) return
    for (const key of ['modelId', 'elementId', 'range']) if (Object.hasOwn(selection, key)) state[key] = selection[key]
    state.response = null
    state.selectedTime = ''
    state.error = ''
    state.source = ''
    const range = state.range
    if (!state.dictionariesReady || !Array.isArray(range) || range.length !== 2 ||
        !range.every(value => typeof value === 'string' && /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(value)) || range[0] > range[1]) {
      state.status = 'error'
      state.error = '请选择有效的起止时间，开始时间不能晚于结束时间。'
      return
    }
    const params = { modelId: state.modelId, elementId: state.elementId, startTime: range[0], endTime: range[1] }
    const key = buildWorkbenchCacheKey(params)
    state.status = 'loading'
    try {
      const cached = cache.get(key)
      const data = cached ?? await api.fetchWorkbench(params)
      if (disposed || version !== requestVersion) return
      if (!data?.model || !data?.element || !Array.isArray(data.times) || !Array.isArray(data.records) ||
          data.model.id !== params.modelId || data.element.id !== params.elementId) throw new Error('Invalid response')
      cache.set(key, data)
      state.response = data
      state.selectedTime = data.times[0] ?? ''
      state.source = cached ? 'cache' : 'network'
      state.status = data.times.length && data.records.length ? 'success' : 'empty'
    } catch {
      if (disposed || version !== requestVersion) return
      state.status = 'error'
      // Do not display raw server errors: these can contain connection details.
      state.error = '气象数据加载失败，请确认后端服务与查询参数，然后重试。'
    }
  }

  function selectTime(time) {
    if (state.status === 'success' && state.response.times.includes(time)) state.selectedTime = time
  }

  function dispose() {
    disposed = true
    requestVersion++
    cache.clear()
  }

  return { state, initialize, load, selectTime, dispose }
}
