<script setup>
import { reactive, ref } from 'vue'
import CityManagement from '../components/management/CityManagement.vue'
import ForecastModelManagement from '../components/management/ForecastModelManagement.vue'
import WeatherElementManagement from '../components/management/WeatherElementManagement.vue'
import ForecastRecordManagement from '../components/management/ForecastRecordManagement.vue'
import '../components/management/management.css'
const active = ref('cities')
const revisions = reactive({ cities: 0, models: 0, elements: 0 })
</script>

<template>
  <section class="management-workspace" aria-label="数据管理">
    <header class="management-heading">
      <p class="eyebrow">SHANDONG / DATA MANAGEMENT</p>
      <h1>数据管理</h1>
      <p>维护城市、模型、要素与预报记录 · 修改将保存到真实数据库</p>
    </header>
    <el-tabs v-model="active">
      <el-tab-pane label="城市" name="cities" lazy><CityManagement @changed="revisions.cities++" /></el-tab-pane>
      <el-tab-pane label="预报模型" name="models" lazy><ForecastModelManagement @changed="revisions.models++" /></el-tab-pane>
      <el-tab-pane label="气象要素" name="elements" lazy><WeatherElementManagement @changed="revisions.elements++" /></el-tab-pane>
      <el-tab-pane label="预报记录" name="records" lazy><ForecastRecordManagement :active="active === 'records'" :revisions="revisions" /></el-tab-pane>
    </el-tabs>
  </section>
</template>
