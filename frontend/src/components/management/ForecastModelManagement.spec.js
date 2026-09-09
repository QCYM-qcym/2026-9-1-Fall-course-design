import { vi } from 'vitest'
import { crudCases } from './crudTestHelpers.js'
const api=vi.hoisted(()=>({list:vi.fn(),create:vi.fn(),update:vi.fn(),remove:vi.fn()}))
vi.mock('../../api/management.js',()=>({fetchForecastModels:api.list,createForecastModel:api.create,updateForecastModel:api.update,deleteForecastModel:api.remove}))
import Component from './ForecastModelManagement.vue'
crudCases('Forecast model',Component,api,{id:41,modelCode:'TMP_MODEL',modelName:'Test model',description:'Test description'},
 {modelCode:'TMP_MODEL',modelName:'Test model',description:'Test description'},
 {modelCode:'TMP_MODEL',modelName:'Test model',description:'Test description'})
