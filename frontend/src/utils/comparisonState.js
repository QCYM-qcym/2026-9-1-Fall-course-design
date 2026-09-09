import { reactive } from 'vue'
import { DEMO_RANGE, selectDefault, resolveGeoName } from './weatherWorkbench.js'
import { buildComparisonQuery, isComparisonResponse, isComparisonEmpty } from './comparisonAnalysis.js'

export function createComparisonState(api) {
  let requestVersion = 0
  let disposed = false
  const state = reactive({ cities: [], elements: [], cityId: null, elementId: null, range: [...DEMO_RANGE],
    dictionariesReady: false, status: 'idle', error: '', warning: '', response: null, appliedQuery: null })

  async function initialize() {
    if (disposed) return
    const version = ++requestVersion
    Object.assign(state, { status: 'loading', error: '', response: null, appliedQuery: null })
    try {
      const [allCities, allElements] = await Promise.all([api.fetchCities(), api.fetchWeatherElements()])
      if (disposed || version !== requestVersion) return
      const cities = allCities.filter(c => resolveGeoName(c.cityCode))
      const elements = allElements.filter(e => ['T2M', 'PRECIP'].includes(e.elementCode))
      const city = cities.find(c => c.id === state.cityId) ?? selectDefault(cities, 'cityCode', 'JINAN')
      const element = elements.find(e => e.id === state.elementId) ?? selectDefault(elements, 'elementCode', 'T2M')
      if (!city || !element) throw new Error('No supported dictionaries')
      Object.assign(state, { cities, elements, cityId: city.id, elementId: element.id, dictionariesReady: true })
      state.warning = !cities.some(c => c.cityCode === 'JINAN') || !elements.some(e => e.elementCode === 'T2M')
        ? '默认演示编码缺失，已选择字典中第一项可用的核心城市或要素。' : ''
      await load()
    } catch {
      if (disposed || version !== requestVersion) return
      state.status = 'error'; state.error = '城市或气象要素加载失败，或没有可用的核心选项。请重试。'
    }
  }

  async function load() {
    if (disposed) return
    const version = ++requestVersion
    Object.assign(state, { response: null, appliedQuery: null, error: '' })
    let params
    try { params = buildComparisonQuery(state) }
    catch (error) { state.status = 'error'; state.error = error.message; return }
    if (!state.dictionariesReady) { state.status = 'error'; state.error = '请先加载城市与气象要素。'; return }
    state.status = 'loading'
    try {
      const data = await api.fetchComparison(params)
      if (disposed || version !== requestVersion) return
      if (!isComparisonResponse(data) || data.cityId !== params.cityId || data.element.id !== params.elementId) throw new Error('Invalid comparison response')
      state.response = data; state.appliedQuery = params
      state.status = isComparisonEmpty(data) ? 'empty' : 'success'
    } catch {
      if (disposed || version !== requestVersion) return
      state.status = 'error'; state.error = '模型对比数据加载失败，请确认后端服务与查询条件后重试。'
    }
  }

  function retry() { return state.dictionariesReady ? load() : initialize() }
  function dispose() { disposed = true; requestVersion++ }
  return { state, initialize, load, retry, dispose }
}
