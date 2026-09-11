import { createRouter, createWebHistory } from 'vue-router'
import { auth } from '../auth/index.js'
import { createAuthGuard } from '../utils/authGuard.js'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
    { path: '/', redirect: '/weather' },
    { path: '/weather', component: () => import('../views/WeatherWorkbench.vue') },
    { path: '/analysis', component: () => import('../views/TrendAnalysis.vue') },
    { path: '/comparison', component: () => import('../views/ModelComparison.vue') },
    { path: '/management', meta: { requiresAdmin: true }, component: () => import('../views/DataManagement.vue') }
  ]
})
router.beforeEach(createAuthGuard(auth))

export default router
