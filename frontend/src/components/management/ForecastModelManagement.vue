<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchForecastModels, createForecastModel, updateForecastModel, deleteForecastModel } from '../../api/management.js'
import { modelPayload, displayDescription, copyDraft, managementError } from '../../utils/management.js'
import { createManagementList } from '../../utils/managementState.js'

const emit=defineEmits(['changed'])
const list=createManagementList(fetchForecastModels)
const {state}=list
const dialog=ref(false), draft=ref({}), editingId=ref(null), deletingId=ref(null), operationError=ref(''), notice=ref('')
let alive=true
function open(row=null) {
  if(state.busy || deletingId.value!==null) return
  editingId.value=row?.id ?? null
  draft.value=row ? copyDraft(row) : {modelCode:'',modelName:'',description:''}
  operationError.value='';notice.value='';dialog.value=true
}
function cancel() { if(!state.busy) dialog.value=false }
async function save() {
  if(state.busy || deletingId.value!==null) return
  let body
  try {body=modelPayload(draft.value)} catch(error) {operationError.value=error.message;return}
  operationError.value='';notice.value=''
  try {
    const result=await list.write(()=>editingId.value===null ? createForecastModel(body) : updateForecastModel(editingId.value,body),()=>{
      dialog.value=false;emit('changed');ElMessage.success({message:'保存成功',customClass:'management-message'})
    })
    if(result && !result.refreshed) notice.value='保存成功，但列表刷新失败，请重试刷新。'
  } catch(error) {if(alive) {operationError.value=managementError(error);ElMessage.error({message:operationError.value,customClass:'management-message'})}}
}
async function remove(row) {
  if(state.busy || deletingId.value!==null) return
  deletingId.value=row.id;operationError.value='';notice.value=''
  try {
    await ElMessageBox.confirm('确认删除预报模型“'+row.modelName+'”吗？','删除确认',{type:'warning',confirmButtonText:'确认删除',cancelButtonText:'取消',customClass:'management-confirm'})
    if(!alive) return
    const result=await list.write(()=>deleteForecastModel(row.id),()=>{emit('changed');ElMessage.success({message:'删除成功',customClass:'management-message'})})
    if(result && !result.refreshed) notice.value='删除成功，但列表刷新失败，请重试刷新。'
  } catch(error) {if(alive && error!=='cancel' && error!=='close') operationError.value=managementError(error)}
  finally {if(alive) deletingId.value=null}
}
onMounted(list.load)
onBeforeUnmount(()=>{alive=false;list.dispose()})
</script>

<template>
  <section class="management-panel" aria-label="预报模型管理" :aria-busy="state.status==='loading'">
    <header class="management-toolbar"><div><h2>预报模型管理</h2><p>维护模型编码、名称和可选描述。</p></div><div class="management-actions"><el-button @click="list.load" :disabled="state.busy">刷新</el-button><el-button data-test="create" type="primary" :disabled="state.busy || deletingId!==null" @click="open()">新增预报模型</el-button></div></header>
    <p v-if="notice" role="status" class="management-notice">{{ notice }}</p>
    <p v-if="operationError && !dialog" role="alert" class="management-error">{{ operationError }}</p>
    <div v-if="state.status==='error'" role="alert" class="management-error">{{ state.error }} <el-button data-test="retry" @click="list.load">重试</el-button></div>
    <p v-if="state.status==='loading'" role="status">正在加载预报模型数据…</p>
    <el-table :data="state.rows" row-key="id" max-height="480" empty-text="暂无预报模型数据" class="management-table">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="modelCode" label="预报模型编码" min-width="170" />
      <el-table-column prop="modelName" label="预报模型名称" min-width="130" />
      <el-table-column label="描述" min-width="240"><template #default="{row}">{{ displayDescription(row.description) }}</template></el-table-column>
      <el-table-column label="操作" width="155" fixed="right"><template #default="{row}"><el-button data-test="edit" link type="primary" :disabled="state.busy || deletingId!==null" @click="open(row)">编辑</el-button><el-button data-test="delete" link type="danger" :disabled="state.busy || deletingId!==null" @click="remove(row)">删除</el-button></template></el-table-column>
    </el-table>
    <el-dialog v-model="dialog" :title="editingId===null?'新增预报模型':'编辑预报模型'" class="management-dialog" width="560px" destroy-on-close :close-on-click-modal="false" :close-on-press-escape="!state.busy" :show-close="!state.busy">
      <form v-if="dialog" class="management-form" @submit.prevent="save">
        <label>预报模型编码<el-input v-model="draft.modelCode" name="modelCode" maxlength="32" :disabled="state.busy" /></label>
        <label>预报模型名称<el-input v-model="draft.modelName" name="modelName" maxlength="50" :disabled="state.busy" /></label>
        <label>描述（可选）<el-input v-model="draft.description" name="description" type="textarea" maxlength="255" :rows="3" :disabled="state.busy" /></label>
        <p v-if="operationError" role="alert" class="management-error">{{ operationError }}</p>
        <div class="management-dialog-actions"><el-button data-test="cancel" :disabled="state.busy" @click="cancel">取消</el-button><el-button data-test="submit" native-type="submit" type="primary" :loading="state.busy" :disabled="state.busy">保存</el-button></div>
      </form>
    </el-dialog>
  </section>
</template>
