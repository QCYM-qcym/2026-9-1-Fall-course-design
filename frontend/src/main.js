import { createApp } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/global.css'
import App from './App.vue'
import router from './router'

use([CanvasRenderer])

createApp(App).use(router).use(ElementPlus).mount('#app')
