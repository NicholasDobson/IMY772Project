import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'

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
      component: () => import('../views/BlogView.vue'),
    },
    {
      path: '/upload',
      name: 'upload',
      component: () => import('../views/DataUploadView.vue'),
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

export default router
