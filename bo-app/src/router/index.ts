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
    { path: '/', name: 'queue', component: () => import('@/views/QueueView.vue') },
    {
      path: '/requests/:id(\\d+)',
      name: 'request',
      component: () => import('@/views/RequestDetailView.vue'),
      props: (route) => ({ id: Number(route.params.id) }),
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

// Logged-out users go to login; logged-in users skip the login page
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.guestOnly) {
    return auth.hasValidSession() ? { name: 'queue' } : true
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
