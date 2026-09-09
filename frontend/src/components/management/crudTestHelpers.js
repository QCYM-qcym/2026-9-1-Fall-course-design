import { mount, flushPromises, DOMWrapper } from '@vue/test-utils'
import ElementPlus, { ElMessage, ElMessageBox } from 'element-plus'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
export const pending=()=>{let resolve,reject;const promise=new Promise((a,b)=>{resolve=a;reject=b});return{promise,resolve,reject}}
export function crudCases(label,Component,api,row,fields,payload) {
 describe(label,()=>{
  let wrapper,confirm,mounted
  const start=async()=>{mounted=mount(Component,{attachTo:document.body,global:{plugins:[ElementPlus]}});wrapper=new DOMWrapper(document.body);await flushPromises();return wrapper}
  const click=async name=>{await wrapper.get('[data-test="'+name+'"]').trigger('click');await flushPromises();if(name==='create'||name==='edit')await vi.waitFor(()=>expect(wrapper.find('form').exists()).toBe(true))}
  const fill=async()=>{for(const [key,value]of Object.entries(fields)) await wrapper.get('input[name="'+key+'"],textarea[name="'+key+'"]').setValue(value)}
  const submit=async()=>{await wrapper.get('form').trigger('submit');await flushPromises()}
  beforeEach(()=>{vi.clearAllMocks();api.list.mockResolvedValue([row]);api.create.mockResolvedValue({...row,id:90});api.update.mockResolvedValue(row);api.remove.mockResolvedValue(null);confirm=vi.spyOn(ElMessageBox,'confirm').mockResolvedValue('confirm')})
  afterEach(()=>{mounted?.unmount();ElMessage.closeAll();confirm.mockRestore();document.body.innerHTML=''})
  it('shows initial loading then real row',async()=>{const d=pending();api.list.mockReturnValueOnce(d.promise);await start();expect(wrapper.text()).toContain('加载');d.resolve([row]);await flushPromises();expect(wrapper.text()).toContain(Object.values(fields)[1])})
  it('shows empty state',async()=>{api.list.mockResolvedValue([]);await start();expect(wrapper.text()).toContain('暂无')})
  it('GET failure is local and Retry recovers',async()=>{api.list.mockRejectedValueOnce(Error('offline'));await start();expect(wrapper.find('[role="alert"]').exists()).toBe(true);await click('retry');expect(wrapper.text()).toContain(Object.values(fields)[1]);expect(api.list).toHaveBeenCalledTimes(2)})
  it('creates from a dialog with exact payload',async()=>{await start();await click('create');await fill();await submit();expect(api.create).toHaveBeenCalledWith(payload);expect(api.list).toHaveBeenCalledTimes(2);expect(wrapper.find('form').exists()).toBe(false)})
  it('failed create keeps dialog and input',async()=>{api.create.mockRejectedValue({response:{status:409,data:{message:'duplicate code'}}});await start();await click('create');await fill();await submit();expect(wrapper.find('form').exists()).toBe(true);expect(wrapper.text()).toContain('duplicate code');expect(wrapper.get('input[name="'+Object.keys(fields)[0]+'"]').element.value).toBe(Object.values(fields)[0])})
  it('edit is prefilled and cancel never mutates row',async()=>{await start();await click('edit');const key=Object.keys(fields)[1];expect(wrapper.get('input[name="'+key+'"]').element.value).toBe(fields[key]);await wrapper.get('input[name="'+key+'"]').setValue('Changed');await click('cancel');expect(wrapper.text()).toContain(fields[key]);expect(api.update).not.toHaveBeenCalled()})
  it('edit submits without response-only fields',async()=>{await start();await click('edit');await submit();expect(api.update).toHaveBeenCalledWith(row.id,payload)})
  it('delete cancellation sends nothing',async()=>{confirm.mockRejectedValue('cancel');await start();await click('delete');expect(api.remove).not.toHaveBeenCalled();expect(wrapper.text()).toContain(Object.values(fields)[1])})
  it('confirmed delete refreshes only after backend success',async()=>{const d=pending();api.remove.mockReturnValueOnce(d.promise);await start();await click('delete');expect(confirm).toHaveBeenCalled();expect(wrapper.text()).toContain(Object.values(fields)[1]);expect(api.list).toHaveBeenCalledTimes(1);d.resolve(null);await flushPromises();expect(api.remove).toHaveBeenCalledWith(row.id);expect(api.list).toHaveBeenCalledTimes(2)})
  it('delete 409 retains table row',async()=>{api.remove.mockRejectedValue({response:{status:409,data:{message:'resource is referenced'}}});await start();await click('delete');expect(wrapper.text()).toContain(Object.values(fields)[1]);expect(wrapper.text()).toContain('resource is referenced')})
  it('save-success/refresh-failure is not reported as save failure',async()=>{await start();api.list.mockRejectedValue(Error('refresh failed'));await click('create');await fill();await submit();expect(wrapper.find('form').exists()).toBe(false);expect(wrapper.text()).toContain('保存成功，但列表刷新失败');expect(api.create).toHaveBeenCalledTimes(1)})
  it('busy submit cannot issue a duplicate write',async()=>{const d=pending();api.create.mockReturnValueOnce(d.promise);await start();await click('create');await fill();await submit();await submit();expect(api.create).toHaveBeenCalledTimes(1);expect(wrapper.get('[data-test="submit"]').attributes('disabled')).toBeDefined();d.resolve(row);await flushPromises()})
  it('write failure 400/404/500 keeps dialog',async()=>{await start();await click('create');await fill();for(const status of [400,404,500]){api.create.mockRejectedValue({response:{status}});await submit();expect(wrapper.find('form').exists()).toBe(true)}})
 })
}
