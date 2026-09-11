export function createAuthGuard(auth) {
  return async to => {
    try { await auth.restore() } catch { /* Login shows the connection error, not a navigation rejection. */ }
    if (to.name === 'login') return auth.state.currentUser ? '/weather' : true
    if (!auth.state.currentUser) return '/login'
    if (to.meta?.requiresAdmin && auth.state.currentUser.role !== 'ADMIN') {
      auth.state.notice = '当前账号没有数据管理权限。'
      return '/weather'
    }
    return true
  }
}
