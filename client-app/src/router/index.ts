import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    guestOnly?: boolean
    /** Open to everyone, logged in or not (e.g. the link from the verification email) */
    public?: boolean
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
    {
      path: '/check-email',
      name: 'check-email',
      component: () => import('@/views/CheckEmailView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/verify',
      name: 'verify',
      component: () => import('@/views/VerifyEmailView.vue'),
      meta: { public: true },
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
  if (to.meta.public) {
    return true
  }
  if (to.meta.guestOnly) {
    return auth.hasValidSession() ? { name: 'dashboard' } : true
  }
  if (!auth.hasValidSession()) {
    // A token that is still stored but past its expiry means the session ended
    const expired = auth.token !== null
    if (expired) auth.logout()
    return { name: 'login', query: { redirect: to.fullPath, ...(expired ? { expired: '1' } : {}) } }
  }
  return true
})

export default router
