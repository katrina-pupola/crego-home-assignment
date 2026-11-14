<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import AuthLayout from '@/components/AuthLayout.vue'
import FormField from '@/components/FormField.vue'
import { toApiError } from '@/api/errors'
import { useAuthStore } from '@/stores/auth'
import { isStrongPassword, passwordRules } from '@/utils/validation'

const auth = useAuthStore()
const router = useRouter()

const form = reactive({ fullName: '', email: '', password: '', confirmPassword: '' })
const fieldErrors = ref<Record<string, string>>({})
const error = ref('')
const submitting = ref(false)

function validate(): Record<string, string> {
  const errors: Record<string, string> = {}
  if (!form.fullName.trim()) errors.fullName = 'Name is required'
  if (!/^\S+@\S+\.\S+$/.test(form.email)) errors.email = 'Enter a valid email address'
  if (!isStrongPassword(form.password)) errors.password = 'Password does not meet all requirements'
  if (form.confirmPassword !== form.password) errors.confirmPassword = 'Passwords do not match'
  return errors
}

async function submit() {
  error.value = ''
  fieldErrors.value = validate()
  if (Object.keys(fieldErrors.value).length > 0) return

  submitting.value = true
  try {
    await auth.register(form.fullName, form.email, form.password)
    await router.push('/')
  } catch (e) {
    const apiError = toApiError(e)
    error.value = apiError.message
    fieldErrors.value = apiError.fieldErrors
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthLayout>
    <h1>Create an account</h1>
    <p class="muted">It only takes a minute.</p>
    <p v-if="error" class="alert alert-error" role="alert">{{ error }}</p>

    <form novalidate @submit.prevent="submit">
      <FormField
        id="fullName"
        v-model="form.fullName"
        label="Full name"
        autocomplete="name"
        :error="fieldErrors.fullName"
      />
      <FormField
        id="email"
        v-model="form.email"
        label="Email"
        type="email"
        autocomplete="email"
        inputmode="email"
        :error="fieldErrors.email"
      />
      <FormField
        id="password"
        v-model="form.password"
        label="Password"
        type="password"
        autocomplete="new-password"
        :error="fieldErrors.password"
      />
      <ul class="password-rules" aria-label="Password requirements">
        <li
          v-for="rule in passwordRules"
          :key="rule.label"
          :class="{ met: rule.test(form.password) }"
        >
          {{ rule.label }}
        </li>
      </ul>
      <FormField
        id="confirmPassword"
        v-model="form.confirmPassword"
        label="Confirm password"
        type="password"
        autocomplete="new-password"
        :error="fieldErrors.confirmPassword"
      />
      <button class="btn btn-primary btn-block" type="submit" :disabled="submitting">
        {{ submitting ? 'Creating account…' : 'Register' }}
      </button>
    </form>

    <p class="muted center">Already registered? <RouterLink to="/login">Log in</RouterLink></p>
  </AuthLayout>
</template>
