<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import AuthLayout from '@/components/AuthLayout.vue'
import FormField from '@/components/FormField.vue'
import ResendVerification from '@/components/ResendVerification.vue'
import { toApiError } from '@/api/errors'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const email = ref('')
const password = ref('')
const error = ref('')
const fieldErrors = ref<Record<string, string>>({})
const notVerified = ref(false)
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
  fieldErrors.value = {}
  notVerified.value = false
  submitting.value = true
  try {
    await auth.login(email.value, password.value)
    await router.push(redirectTarget())
  } catch (e) {
    const apiError = toApiError(e)
    error.value = apiError.message
    fieldErrors.value = apiError.fieldErrors
    notVerified.value = apiError.code === 'EMAIL_NOT_VERIFIED'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthLayout>
    <h1>Welcome back</h1>
    <p class="muted">Log in to manage your payment method.</p>
    <p v-if="sessionExpired && !error" class="alert alert-info">
      Your session expired. Please log in again.
    </p>
    <template v-if="notVerified">
      <p class="alert alert-info" role="alert">{{ error }}</p>
      <ResendVerification :email="email" />
      <hr class="divider" />
    </template>
    <p v-else-if="error" class="alert alert-error" role="alert">{{ error }}</p>

    <form novalidate @submit.prevent="submit">
      <FormField
        id="email"
        v-model="email"
        label="Email"
        type="email"
        autocomplete="email"
        inputmode="email"
        :error="fieldErrors.email"
      />
      <FormField
        id="password"
        v-model="password"
        label="Password"
        type="password"
        autocomplete="current-password"
        :error="fieldErrors.password"
      />
      <button class="btn btn-primary btn-block" type="submit" :disabled="submitting">
        {{ submitting ? 'Logging in…' : 'Log in' }}
      </button>
    </form>

    <p class="muted center">No account yet? <RouterLink to="/register">Register</RouterLink></p>
  </AuthLayout>
</template>
