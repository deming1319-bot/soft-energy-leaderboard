import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
    {
      path: '/',
      component: () => import('@/layouts/AdminShell.vue'),
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', name: 'dashboard', component: () => import('@/views/DashboardView.vue') },
        { path: 'activities', name: 'activities', component: () => import('@/views/ActivitiesView.vue') },
        { path: 'activities/new', name: 'activity-create', component: () => import('@/views/ActivityEditorView.vue') },
        { path: 'activities/:id/edit', name: 'activity-edit', component: () => import('@/views/ActivityEditorView.vue') },
        { path: 'activities/:id', name: 'activity-detail', component: () => import('@/views/ActivityDetailView.vue') },
        { path: 'test-center', name: 'test-center', component: () => import('@/views/TestCenterView.vue') },
        { path: 'users', name: 'users', component: () => import('@/views/UsersView.vue') },
        { path: 'reports', name: 'reports', component: () => import('@/views/ReportsView.vue') },
        { path: 'audit', name: 'audit', component: () => import('@/views/AuditView.vue') },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem('soft-energy-admin-token')
  if (!to.meta.public && !token) return { name: 'login', query: { redirect: to.fullPath } }
  if (to.name === 'login' && token) return { name: 'dashboard' }
  return true
})

export default router
