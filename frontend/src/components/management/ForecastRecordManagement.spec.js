import { mount, flushPromises, DOMWrapper } from '@vue/test-utils'
import ElementPlus, { ElSelect, ElDatePicker, ElMessage, ElMessageBox } from 'element-plus'
import { beforeEach, afterEach, expect, it, vi } from 'vitest'
import { pending } from './crudTestHelpers.js'
const api=vi.hoisted(()=>({fetchCities:vi.fn(),fetchForecastModels:vi.fn(),fetchWeatherElements:vi.fn(),fetchForecastRecords:vi.fn(),createForecastRecord:vi.fn(),updateForecastRecord:vi.fn(),deleteForecastRecord:vi.fn()}))
vi.mock('../../api/management.js',()=>api)
import Component from './ForecastRecordManagement.vue'
const row={id:90,cityId:8,cityCode:'TMP_CITY',cityName:'Test city',modelId:41,modelCode:'TMP_MODEL',modelName:'Test model',elementId:71,elementCode:'PRECIP',elementName:'降水量',unit:'mm',forecastTime:'2026-09-07 08:00:00',value:0}
let wrapper,dom,confirm
async function start(){wrapper=mount(Component,{props:{active:true,revisions:{cities:0,models:0,elements:0}},attachTo:document.body,global:{plugins:[ElementPlus]}});dom=new DOMWrapper(document.body);await flushPromises()}
async function click(name){await dom.get('[data-test="'+name+'"]').trigger('click');await flushPromises()}
async function fill(){
 const selects=wrapper.findAllComponents(ElSelect)
 selects[0].vm.$emit('update:modelValue',8);selects[1].vm.$emit('update:modelValue',41);selects[2].vm.$emit('update:modelValue',71)
 wrapper.findComponent(ElDatePicker).vm.$emit('update:modelValue','2026-09-07 08:00:00')
 await flushPromises();await dom.get('input[name="value"]').setValue('0.00')
}
async function submit(){await dom.get('form').trigger('submit');await flushPromises()}
beforeEach(()=>{vi.clearAllMocks();api.fetchCities.mockResolvedValue([{id:8,cityCode:'TMP_CITY',cityName:'Test city'}]);api.fetchForecastModels.mockResolvedValue([{id:41,modelCode:'TMP_MODEL',modelName:'Test model'}]);api.fetchWeatherElements.mockResolvedValue([{id:71,elementCode:'PRECIP',elementName:'降水量',unit:'mm'}]);api.fetchForecastRecords.mockResolvedValue([row]);api.createForecastRecord.mockResolvedValue(row);api.updateForecastRecord.mockResolvedValue(row);api.deleteForecastRecord.mockResolvedValue(null);confirm=vi.spyOn(ElMessageBox,'confirm').mockResolvedValue('confirm')})
afterEach(()=>{wrapper?.unmount();ElMessage.closeAll();confirm.mockRestore();document.body.innerHTML=''})
it('renders extended VO including names units time and real zero',async()=>{await start();for(const value of ['Test city','Test model','降水量','mm','2026-09-07 08:00:00','0.00'])expect(dom.text()).toContain(value)})
it('creates with dictionary numeric IDs and local time only',async()=>{await start();await click('create');await fill();await submit();expect(api.createForecastRecord).toHaveBeenCalledWith({cityId:8,modelId:41,elementId:71,forecastTime:'2026-09-07 08:00:00',value:0});expect(api.fetchForecastRecords).toHaveBeenCalledTimes(2)})
it('edit prefill preserves IDs and excludes VO metadata from PUT',async()=>{await start();await click('edit');expect(wrapper.findAllComponents(ElSelect).map(c=>c.props('modelValue'))).toEqual([8,41,71]);await submit();expect(api.updateForecastRecord).toHaveBeenCalledWith(90,{cityId:8,modelId:41,elementId:71,forecastTime:'2026-09-07 08:00:00',value:0})})
it('rejects negative PRECIP and retains dialog',async()=>{await start();await click('create');await fill();await dom.get('input[name="value"]').setValue('-0.01');await submit();expect(api.createForecastRecord).not.toHaveBeenCalled();expect(dom.text()).toContain('降水量不能为负数')})
it('duplicate 409 retains draft and visible row',async()=>{api.createForecastRecord.mockRejectedValue({response:{status:409,data:{message:'forecast record already exists'}}});await start();await click('create');await fill();await submit();expect(dom.text()).toContain('forecast record already exists');expect(dom.get('input[name="value"]').element.value).toBe('0.00');expect(dom.find('form').exists()).toBe(true)})
it('confirmed deletion calls correct ID and refreshes',async()=>{await start();await click('delete');expect(api.deleteForecastRecord).toHaveBeenCalledWith(90);expect(api.fetchForecastRecords).toHaveBeenCalledTimes(2)})
it('shows list empty and loading independently of dictionaries',async()=>{const d=pending();api.fetchForecastRecords.mockReturnValueOnce(d.promise);await start();expect(dom.text()).toContain('加载');d.resolve([]);await flushPromises();expect(dom.text()).toContain('暂无预报记录')})
it('list Retry does not refetch successful dictionaries',async()=>{api.fetchForecastRecords.mockRejectedValueOnce(Error('offline'));await start();await click('retry');expect(api.fetchForecastRecords).toHaveBeenCalledTimes(2);expect(api.fetchCities).toHaveBeenCalledTimes(1)})
it('dictionary retry reloads only failed dictionary',async()=>{api.fetchForecastModels.mockRejectedValueOnce(Error('offline'));await start();expect(dom.text()).toContain('预报记录依赖字典加载失败');await click('retry-dictionaries');expect(api.fetchForecastModels).toHaveBeenCalledTimes(2);expect(api.fetchCities).toHaveBeenCalledTimes(1);expect(api.fetchWeatherElements).toHaveBeenCalledTimes(1)})
it('inactive dictionary mutation invalidates metadata and reloads on activation',async()=>{await start();await wrapper.setProps({active:false});await wrapper.setProps({revisions:{cities:1,models:0,elements:0}});expect(api.fetchCities).toHaveBeenCalledTimes(1);await wrapper.setProps({active:true});await flushPromises();expect(api.fetchCities).toHaveBeenCalledTimes(2);expect(api.fetchForecastModels).toHaveBeenCalledTimes(1);expect(api.fetchForecastRecords).toHaveBeenCalledTimes(2)})
it('stale list cannot overwrite latest request',async()=>{await start();const a=pending(),b=pending();api.fetchForecastRecords.mockReturnValueOnce(a.promise).mockReturnValueOnce(b.promise);await click('refresh');await click('refresh');b.resolve([{...row,cityName:'New city'}]);await flushPromises();a.resolve([row]);await flushPromises();expect(dom.text()).toContain('New city');expect(dom.text()).not.toContain('Test city')})
