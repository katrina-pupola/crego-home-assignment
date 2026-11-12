import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    guestOnly?: boolean
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { guestOnly: true },
    },
    { path: '/', name: 'dashboard', component: () => import('@/views/DashboardView.vue') },
    {
      path: '/payment-method/new',
      name: 'payment-method-new',
      component: () => import('@/views/PaymentMethodFormView.vue'),
    },
    {
      path: '/requests',
      name: 'requests',
      component: () => import('@/views/RequestHistoryView.vue'),
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

// Logged-out users go to login; logged-in users skip login and register
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.guestOnly) {
    return auth.isLoggedIn ? { name: 'dashboard' } : true
  }
  if (!auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
