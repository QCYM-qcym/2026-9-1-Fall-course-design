import { validLocalTime } from './trendAnalysis.js'

const fallbacks = {400:'输入数据不合法',404:'数据不存在或已被删除',409:'当前操作存在数据冲突',500:'服务器处理失败'}
export function managementError(error) {
  const status = error?.response?.status ?? error?.code
  const message = error?.response?.data?.message
  if (typeof message === 'string' && message.trim() && message.length <= 240 &&
      !/[<>\r\n]|SQLException|jdbc:|password|stacktrace/i.test(message)) return message
  return fallbacks[status] ?? (error?.response ? '服务器处理失败' : '无法连接后端服务')
}
export const displayDescription = value => typeof value === 'string' && value.trim() ? value : '--'
export const copyDraft = row => ({...row})

function text(value, max, label) {
  if(typeof value !== 'string' || !value.trim() || value.length > max) throw new Error(label + '必填且最多 ' + max + ' 个字符')
  return value.trim()
}
function code(value) {
  const result=text(value,32,'编码')
  if(!/^[A-Z0-9_]+$/.test(result)) throw new Error('编码仅允许大写字母、数字和下划线')
  return result
}
function decimal(value, min, max, precision, label) {
  if(value === null || value === undefined || String(value).trim() === '') throw new Error(label + '不能为空')
  const str=String(value).trim()
  if(!/^-?\d+(\.\d+)?$/.test(str) || (str.split('.')[1]?.length ?? 0)>precision) throw new Error(label + '小数位数不合法')
  const number=Number(str)
  if(!Number.isFinite(number) || number<min || number>max) throw new Error(label + '超出允许范围')
  return number
}
export function cityPayload(draft) {
  return {cityCode:code(draft.cityCode),cityName:text(draft.cityName,50,'城市名称'),
    longitude:decimal(draft.longitude,-180,180,6,'经度'),latitude:decimal(draft.latitude,-90,90,6,'纬度')}
}
export function modelPayload(draft) {
  if(draft.description != null && (typeof draft.description !== 'string' || draft.description.length>255)) throw new Error('描述最多 255 个字符')
  return {modelCode:code(draft.modelCode),modelName:text(draft.modelName,50,'模型名称'),description:draft.description?.trim() || null}
}
export function elementPayload(draft) {
  return {elementCode:code(draft.elementCode),elementName:text(draft.elementName,50,'要素名称'),unit:text(draft.unit,16,'单位')}
}
export function recordPayload(draft,elements) {
  const {cityId,modelId,elementId,forecastTime}=draft
  if(![cityId,modelId,elementId].every(id=>Number.isSafeInteger(id)&&id>0)) throw new Error('请选择有效的城市、模型和要素')
  if(!validLocalTime(forecastTime) || Number(forecastTime.slice(0,4))<1000) throw new Error('请选择有效的本地预报时间')
  const element=elements.find(e=>e.id===elementId)
  if(!element) throw new Error('所选气象要素已不可用，请刷新字典')
  const value=decimal(draft.value,-99999999.99,99999999.99,2,'预报值')
  if(element.elementCode==='PRECIP' && value<0) throw new Error('降水量不能为负数')
  return {cityId,modelId,elementId,forecastTime,value}
}
