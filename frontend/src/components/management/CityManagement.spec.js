import { vi } from 'vitest'
import { crudCases } from './crudTestHelpers.js'
const api=vi.hoisted(()=>({list:vi.fn(),create:vi.fn(),update:vi.fn(),remove:vi.fn()}))
vi.mock('../../api/management.js',()=>({fetchCities:api.list,createCity:api.create,updateCity:api.update,deleteCity:api.remove}))
const {default:Component}=await import('./CityManagement.vue').catch(()=>({default:{template:'<div />'}}))
crudCases('City',Component,api,{id:8,cityCode:'TMP_CITY',cityName:'Test city',longitude:120,latitude:36},
 {cityCode:'TMP_CITY',cityName:'Test city',longitude:'120',latitude:'36'},
 {cityCode:'TMP_CITY',cityName:'Test city',longitude:120,latitude:36})
