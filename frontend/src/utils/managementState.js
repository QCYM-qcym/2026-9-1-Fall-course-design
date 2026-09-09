import { reactive } from 'vue'
import { managementError } from './management.js'

// Local async lifecycle only: no resource schema, forms, global store or cache.
export function createManagementList(fetchList) {
  let version=0, disposed=false
  const state=reactive({rows:[],status:'idle',error:'',busy:false})
  async function load() {
    if(disposed) return false
    const current=++version
    state.status='loading';state.error=''
    try {
      const rows=await fetchList()
      if(disposed || current!==version) return false
      if(!Array.isArray(rows)) throw {response:{status:500}}
      state.rows=rows;state.status=rows.length?'success':'empty';return true
    } catch(error) {
      if(disposed || current!==version) return false
      state.status='error';state.error=managementError(error);return false
    }
  }
  async function write(operation,onSaved=()=>{}) {
    if(disposed || state.busy) return null
    state.busy=true
    try {
      await operation()
      if(disposed) return null
      ++version // invalidate older GET only after the mutation succeeds
      onSaved()
      const refreshed=await load()
      return disposed ? null : {saved:true,refreshed}
    } finally { if(!disposed) state.busy=false }
  }
  function dispose(){disposed=true;version++}
  function invalidate(){if(!disposed){version++;state.status='idle';state.error=''}}
  return {state,load,write,dispose,invalidate}
}
