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
  onUnauthorized: () => {
    auth.logout()
    router.push({ name: 'login', query: { expired: '1' } })
  },
})

app.mount('#app')
