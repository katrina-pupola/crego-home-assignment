<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import FormField from '@/components/FormField.vue'
import { toApiError } from '@/api/errors'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const email = ref('')
const password = ref('')
const error = ref('')
const submitting = ref(false)
const sessionExpired = route.query.expired === '1'

// Only allow redirects within this app (a path like "/requests", not "//evil.com")
function redirectTarget(): string {
  const redirect = route.query.redirect
  return typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')
    ? redirect
    : '/'
}

async function submit() {
  error.value = ''
  submitting.value = true
  try {
    await auth.login(email.value, password.value)
    await router.push(redirectTarget())
  } catch (e) {
    error.value = toApiError(e).message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="auth-card card">
    <h1>Log in</h1>
    <p v-if="sessionExpired && !error" class="alert alert-info">
      Your session expired. Please log in again.
    </p>
    <p v-if="error" class="alert alert-error" role="alert">{{ error }}</p>

    <form novalidate @submit.prevent="submit">
      <FormField
        id="email"
        v-model="email"
        label="Email"
        type="email"
        autocomplete="email"
        inputmode="email"
      />
      <FormField
        id="password"
        v-model="password"
        label="Password"
        type="password"
        autocomplete="current-password"
      />
      <button class="btn btn-primary btn-block" type="submit" :disabled="submitting">
        {{ submitting ? 'Logging in…' : 'Log in' }}
      </button>
    </form>

    <p class="muted center">No account yet? <RouterLink to="/register">Register</RouterLink></p>
  </section>
</template>
