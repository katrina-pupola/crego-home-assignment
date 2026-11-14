import './styles/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { configureHttp } from './api/http'
import { useAuthStore } from './stores/auth'

const app = createApp(App)

app.use(createPinia())
app.use(router)

const auth = useAuthStore()
configureHttp({
  getToken: () => auth.token,
  // The backend rejected the token: log out and return to this page after logging in again
  onUnauthorized: () => {
    const current = router.currentRoute.value
    if (current.name === 'login') return // several parallel requests can fail at once
    auth.logout()
    router.push({ name: 'login', query: { expired: '1', redirect: current.fullPath } })
  },
})

app.mount('#app')
