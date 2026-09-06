<template>
  <div class="app-shell">
    <header class="app-header">
      <div>
        <p class="brand-kicker">SHANDONG WEATHER</p>
        <strong>山东省气象预报数据可视化系统</strong>
      </div>
      <nav aria-label="主导航">
        <RouterLink to="/weather">气象工作台</RouterLink>
        <RouterLink to="/analysis">趋势分析</RouterLink>
        <RouterLink to="/comparison">模型对比</RouterLink>
        <RouterLink to="/management">数据管理</RouterLink>
      </nav>
    </header>
    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>
