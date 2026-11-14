<script setup lang="ts">
import { watch } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { initials } from '@/utils/format'

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
      <RouterLink to="/" class="brand">
        <span class="brand-mark" aria-hidden="true">PM</span>
        Payment Methods
      </RouterLink>
      <nav>
        <RouterLink to="/">Dashboard</RouterLink>
        <RouterLink to="/requests">Request history</RouterLink>
      </nav>
      <div class="user">
        <template v-if="auth.profile">
          <span class="avatar" aria-hidden="true">{{ initials(auth.profile.fullName) }}</span>
          <span class="user-name">{{ auth.profile.fullName }}</span>
        </template>
        <button class="btn btn-ghost" type="button" @click="logout">Log out</button>
      </div>
    </div>
  </header>

  <main class="container">
    <RouterView />
  </main>
</template>
