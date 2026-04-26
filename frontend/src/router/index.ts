import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: DashboardView,
    },
    {
      path: '/map',
      name: 'map',
      component: () => import('../views/MapView.vue'),
    },
    {
      path: '/river',
      name: 'river',
      component: () => import('../views/RiverDetailView.vue'),
    },
    {
      path: '/compare',
      name: 'compare',
      component: () => import('../views/ComparisonView.vue'),
    },
    {
      path: '/education',
      name: 'education',
      alias: '/blog',
      component: () => import('../views/BlogView.vue'),
    },
    {
      path: '/upload',
      name: 'upload',
      component: () => import('../views/DataUploadView.vue'),
      meta: { requiresAdmin: true },
    },
    {
      path: '/admin/users',
      name: 'user-management',
      component: () => import('../views/UserManagementView.vue'),
      meta: { requiresAdmin: true },
    },
    {
      path: '/settings',
      name: 'settings',
      component: () => import('../views/SettingsView.vue'),
    },
    {
      path: '/blog/create',
      name: 'create-blog',
      component: () => import('../views/CreateBlogView.vue'),
    },
    {
      path: '/bacteria/:name',
      name: 'bacteria-detail',
      component: () => import('../views/BacteriaDetailView.vue'),
    },
  ],
})

router.beforeEach((to) => {
  if (to.meta.requiresAdmin) {
    const auth = useAuthStore()
    if (!auth.isAdmin) {
      return { name: 'dashboard' }
    }
  }
})

export default router
