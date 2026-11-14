<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import AuthLayout from '@/components/AuthLayout.vue'
import ResendVerification from '@/components/ResendVerification.vue'
import { toApiError } from '@/api/errors'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const error = ref('')

// The email link opens this page; the token is sent with a POST, so mail scanners that
// open links automatically cannot use it up
onMounted(async () => {
  const token = route.query.token
  if (typeof token !== 'string' || !token) {
    error.value = 'This link is incomplete. Please use the full link from the email.'
    return
  }
  try {
    auth.logout() // in case someone else was logged in on this browser
    await auth.verifyEmail(token)
    await router.replace({ name: 'dashboard' })
  } catch (e) {
    error.value = toApiError(e).message
  }
})
</script>

<template>
  <AuthLayout>
    <template v-if="!error">
      <h1>Confirming your email…</h1>
      <p class="muted">One moment, please.</p>
    </template>

    <template v-else>
      <h1>We couldn't confirm your email</h1>
      <p class="alert alert-error" role="alert">{{ error }}</p>
      <p class="muted">Enter your email address and we'll send you a new link.</p>
      <ResendVerification />
      <p class="muted center">Already confirmed? <RouterLink to="/login">Log in</RouterLink></p>
    </template>
  </AuthLayout>
</template>
