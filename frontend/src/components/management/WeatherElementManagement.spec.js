import { vi } from 'vitest'
import { crudCases } from './crudTestHelpers.js'
const api=vi.hoisted(()=>({list:vi.fn(),create:vi.fn(),update:vi.fn(),remove:vi.fn()}))
vi.mock('../../api/management.js',()=>({fetchWeatherElements:api.list,createWeatherElement:api.create,updateWeatherElement:api.update,deleteWeatherElement:api.remove}))
import Component from './WeatherElementManagement.vue'
crudCases('Weather element',Component,api,{id:71,elementCode:'TMP_ELEMENT',elementName:'Test element',unit:'mm'},
 {elementCode:'TMP_ELEMENT',elementName:'Test element',unit:'mm'},
 {elementCode:'TMP_ELEMENT',elementName:'Test element',unit:'mm'})
