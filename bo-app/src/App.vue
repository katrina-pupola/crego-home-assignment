<script setup lang="ts">
import { watch } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useSessionTimeout } from '@/composables/useSessionTimeout'
import { useAuthStore } from '@/stores/auth'
import { initials } from '@/utils/format'

const auth = useAuthStore()
const router = useRouter()
const { showWarning, timeLeft, dismissWarning, logInAgain } = useSessionTimeout()

// Load the name shown in the header whenever a staff user logs in
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
        <span class="brand-text">Payment Methods</span>
        <span class="bo-tag">Back Office</span>
      </RouterLink>
      <nav>
        <RouterLink to="/">Change requests</RouterLink>
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

  <ConfirmDialog
    :open="showWarning"
    title="Your session is about to expire"
    :message="`For your security you will be logged out in ${timeLeft}. Logging in again starts a new session; an unfinished decision on this page will be lost.`"
    confirm-label="Log in again"
    cancel-label="Dismiss"
    @confirm="logInAgain"
    @cancel="dismissWarning"
  />
</template>
