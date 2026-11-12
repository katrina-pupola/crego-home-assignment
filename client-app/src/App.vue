<script setup lang="ts">
import { watch } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

// Load the name shown in the header whenever a user logs in
watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    if (loggedIn && !auth.profile) auth.loadProfile().catch(() => {})
  },
  { immediate: true },
)

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <header v-if="auth.isLoggedIn" class="topbar">
    <div class="topbar-inner">
      <RouterLink to="/" class="brand">Payment Methods</RouterLink>
      <nav>
        <RouterLink to="/">Dashboard</RouterLink>
        <RouterLink to="/requests">Request history</RouterLink>
      </nav>
      <div class="user">
        <span v-if="auth.profile">{{ auth.profile.fullName }}</span>
        <button class="btn btn-link" type="button" @click="logout">Log out</button>
      </div>
    </div>
  </header>

  <main class="container">
    <RouterView />
  </main>
</template>
