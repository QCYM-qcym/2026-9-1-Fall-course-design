import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/weather' },
    { path: '/weather', component: () => import('../views/WeatherWorkbench.vue') },
    { path: '/analysis', component: () => import('../views/TrendAnalysis.vue') },
    { path: '/comparison', component: () => import('../views/ModelComparison.vue') },
    { path: '/management', component: () => import('../views/DataManagement.vue') }
  ]
})

export default router
