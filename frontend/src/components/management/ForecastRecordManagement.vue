<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchCities, fetchForecastModels, fetchWeatherElements, fetchForecastRecords, createForecastRecord, updateForecastRecord, deleteForecastRecord } from '../../api/management.js'
import { recordPayload, copyDraft, managementError } from '../../utils/management.js'
import { DEMO_RANGE } from '../../utils/weatherWorkbench.js'
import { formatMetricValue } from '../../utils/trendAnalysis.js'
import { createManagementList } from '../../utils/managementState.js'

const props=defineProps({active:{type:Boolean,default:true},revisions:{type:Object,default:()=>({cities:0,models:0,elements:0})}})
const cities=createManagementList(fetchCities), models=createManagementList(fetchForecastModels), elements=createManagementList(fetchWeatherElements)
const dictionaries={cities,models,elements}
const ready=computed(()=>Object.values(dictionaries).every(d=>['success','empty'].includes(d.state.status)))
const dictionaryError=computed(()=>Object.values(dictionaries).some(d=>d.state.status==='error'))
const dictionaryLoading=computed(()=>Object.values(dictionaries).some(d=>d.state.status==='loading'))
async function retryDictionaries() { await Promise.all(Object.values(dictionaries).filter(d=>['idle','error'].includes(d.state.status)).map(d=>d.load())) }
function ensure() { if(state.status==='idle') list.load(); retryDictionaries() }
watch(()=>[props.revisions.cities,props.revisions.models,props.revisions.elements],(next,previous)=>{
  Object.values(dictionaries).forEach((d,i)=>{if(next[i]!==previous[i]) d.invalidate()})
  list.invalidate()
  if(props.active) ensure()
})
watch(()=>props.active,active=>{if(active)ensure()})
const list=createManagementList(fetchForecastRecords)
const {state}=list
const pageSize=100, page=ref(1)
const pageCount=computed(()=>Math.max(1,Math.ceil(state.rows.length/pageSize)))
const pageRows=computed(()=>state.rows.slice((page.value-1)*pageSize,page.value*pageSize))
watch(pageCount,count=>{page.value=Math.min(page.value,count)},{flush:'sync'})
const dialog=ref(false), draft=ref({}), editingId=ref(null), deletingId=ref(null), operationError=ref(''), notice=ref('')
let alive=true
function open(row=null) {
  if(state.busy || deletingId.value!==null || !ready.value) return
  editingId.value=row?.id ?? null
  draft.value=row ? copyDraft(row) : {cityId:null,modelId:null,elementId:null,forecastTime:DEMO_RANGE[0],value:''}
  operationError.value='';notice.value='';dialog.value=true
}
function cancel() { if(!state.busy) dialog.value=false }
async function save() {
  if(state.busy || deletingId.value!==null) return
  let body
  try {
    if(!ready.value) throw new Error('请先完成字典加载')
    if(!cities.state.rows.some(c=>c.id===draft.value.cityId) || !models.state.rows.some(m=>m.id===draft.value.modelId)) throw new Error('请选择有效的预报记录与模型')
    body=recordPayload(draft.value,elements.state.rows)
  } catch(error) {operationError.value=error.message;return}
  operationError.value='';notice.value=''
  try {
    const result=await list.write(()=>editingId.value===null ? createForecastRecord(body) : updateForecastRecord(editingId.value,body),()=>{
      dialog.value=false;ElMessage.success({message:'保存成功',customClass:'management-message'})
    })
    if(result && !result.refreshed) notice.value='保存成功，但列表刷新失败，请重试刷新。'
  } catch(error) {if(alive) {operationError.value=managementError(error);ElMessage.error({message:operationError.value,customClass:'management-message'})}}
}
async function remove(row) {
  if(state.busy || deletingId.value!==null) return
  deletingId.value=row.id;operationError.value='';notice.value=''
  try {
    await ElMessageBox.confirm('确认删除预报记录 #'+row.id+'（'+row.cityName+' / '+row.forecastTime+'）吗？','删除确认',{type:'warning',confirmButtonText:'确认删除',cancelButtonText:'取消',customClass:'management-confirm'})
    if(!alive) return
    const result=await list.write(()=>deleteForecastRecord(row.id),()=>{ElMessage.success({message:'删除成功',customClass:'management-message'})})
    if(result && !result.refreshed) notice.value='删除成功，但列表刷新失败，请重试刷新。'
  } catch(error) {if(alive && error!=='cancel' && error!=='close') operationError.value=managementError(error)}
  finally {if(alive) deletingId.value=null}
}
onMounted(ensure)
onBeforeUnmount(()=>{alive=false;list.dispose();Object.values(dictionaries).forEach(d=>d.dispose())})
</script>

<template>
  <section class="management-panel" aria-label="预报记录管理" :aria-busy="state.status==='loading'">
    <header class="management-toolbar"><div><h2>预报记录管理</h2><p>全量预报记录 · ID 升序 · 本地业务时间。</p></div><div class="management-actions"><el-button data-test="refresh" @click="list.load" :disabled="state.busy">刷新</el-button><el-button data-test="create" type="primary" :disabled="state.busy || deletingId!==null || !ready" @click="open()">新增预报记录</el-button></div></header>
    <p v-if="notice" role="status" class="management-notice">{{ notice }}</p>
    <p v-if="operationError && !dialog" role="alert" class="management-error">{{ operationError }}</p>
    <div v-if="state.status==='error'" role="alert" class="management-error">{{ state.error }} <el-button data-test="retry" @click="list.load">重试</el-button></div>
    <p v-if="state.status==='loading'" role="status">正在加载预报记录数据…</p>
    <div v-if="dictionaryError" role="alert" class="management-error">预报记录依赖字典加载失败 <el-button data-test="retry-dictionaries" @click="retryDictionaries">重试字典</el-button></div>
    <p v-if="dictionaryLoading" role="status">正在加载城市、模型和要素选项…</p>
    <p v-if="ready && (!cities.state.rows.length || !models.state.rows.length || !elements.state.rows.length)" class="management-notice">字典暂无可选项，请先维护对应字典。</p>
    <el-table :data="pageRows" row-key="id" max-height="480" empty-text="暂无预报记录数据" class="management-table">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="城市" min-width="150"><template #default="{row}">{{ row.cityName }}<small class="management-code">{{ row.cityCode }}</small></template></el-table-column>
      <el-table-column label="模型" min-width="145"><template #default="{row}">{{ row.modelName }}<small class="management-code">{{ row.modelCode }}</small></template></el-table-column>
      <el-table-column label="气象要素" min-width="145"><template #default="{row}">{{ row.elementName }}<small class="management-code">{{ row.elementCode }}</small></template></el-table-column>
      <el-table-column prop="forecastTime" label="预报时间" min-width="185" />
      <el-table-column label="值" width="115"><template #default="{row}">{{ formatMetricValue(row.value) }}</template></el-table-column>
      <el-table-column prop="unit" label="单位" width="75" />
      <el-table-column label="操作" width="155" fixed="right"><template #default="{row}"><el-button data-test="edit" link type="primary" :disabled="state.busy || deletingId!==null || !ready" @click="open(row)">编辑</el-button><el-button data-test="delete" link type="danger" :disabled="state.busy || deletingId!==null" @click="remove(row)">删除</el-button></template></el-table-column>
    </el-table>
    <nav class="management-actions" aria-label="预报记录分页">
      <el-button data-test="page-prev" :disabled="page===1 || state.busy || state.status==='loading' || deletingId!==null" @click="page=Math.max(1,page-1)">上一页</el-button>
      <span data-test="page-status" role="status">{{ page }} / {{ pageCount }} 页 · 共 {{ state.rows.length }} 条 · 每页 {{ pageSize }} 条</span>
      <el-button data-test="page-next" :disabled="page===pageCount || state.busy || state.status==='loading' || deletingId!==null" @click="page=Math.min(pageCount,page+1)">下一页</el-button>
    </nav>
    <el-dialog v-model="dialog" :title="editingId===null?'新增预报记录':'编辑预报记录'" class="management-dialog" width="560px" destroy-on-close :close-on-click-modal="false" :close-on-press-escape="!state.busy" :show-close="!state.busy">
      <form v-if="dialog" class="management-form" @submit.prevent="save">
        <label>城市<el-select v-model="draft.cityId" aria-label="记录城市" :disabled="state.busy || !ready" popper-class="management-popper"><el-option v-for="city in cities.state.rows" :key="city.id" :label="city.cityName+' · '+city.cityCode" :value="city.id" /></el-select></label>
        <label>预报模型<el-select v-model="draft.modelId" aria-label="记录模型" :disabled="state.busy || !ready" popper-class="management-popper"><el-option v-for="model in models.state.rows" :key="model.id" :label="model.modelName+' · '+model.modelCode" :value="model.id" /></el-select></label>
        <label>气象要素<el-select v-model="draft.elementId" aria-label="记录要素" :disabled="state.busy || !ready" popper-class="management-popper"><el-option v-for="element in elements.state.rows" :key="element.id" :label="element.elementName+' · '+element.elementCode+' / '+element.unit" :value="element.id" /></el-select></label>
        <label>预报时间<el-date-picker v-model="draft.forecastTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" aria-label="记录预报时间" :disabled="state.busy" popper-class="management-popper" /></label>
        <label>值<el-input v-model="draft.value" name="value" inputmode="decimal" :disabled="state.busy" /><small>最多2位小数；降水量不能为负，0.00 合法。</small></label>
        <p v-if="operationError" role="alert" class="management-error">{{ operationError }}</p>
        <div class="management-dialog-actions"><el-button data-test="cancel" :disabled="state.busy" @click="cancel">取消</el-button><el-button data-test="submit" native-type="submit" type="primary" :loading="state.busy" :disabled="state.busy">保存</el-button></div>
      </form>
    </el-dialog>
  </section>
</template>
