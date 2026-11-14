<!-- "Resend email" button with a 60 s cooldown (the backend applies the same limit) -->
<script setup lang="ts">
import { onUnmounted, ref } from 'vue'
import FormField from '@/components/FormField.vue'
import * as api from '@/api/client'
import { toApiError } from '@/api/errors'

const COOLDOWN_SECONDS = 60

const props = defineProps<{
  /** When no email is given, the user types it in (e.g. after opening an expired link). */
  email?: string
  /** Use right after an email was sent, because the backend ignores resends within a minute. */
  startWithCooldown?: boolean
}>()

const emailInput = ref(props.email ?? '')
const sending = ref(false)
const sent = ref(false)
const error = ref('')
const cooldown = ref(0)
let timer: ReturnType<typeof setInterval> | undefined

function startCooldown() {
  clearInterval(timer)
  cooldown.value = COOLDOWN_SECONDS
  timer = setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0) clearInterval(timer)
  }, 1000)
}

if (props.startWithCooldown) startCooldown()

async function resend() {
  error.value = ''
  sending.value = true
  try {
    await api.resendVerification(emailInput.value)
    sent.value = true
    startCooldown()
  } catch (e) {
    const apiError = toApiError(e)
    error.value = apiError.fieldErrors.email ?? apiError.message
  } finally {
    sending.value = false
  }
}

onUnmounted(() => clearInterval(timer))
</script>

<template>
  <form class="resend" novalidate @submit.prevent="resend">
    <FormField
      v-if="!email"
      id="resendEmail"
      v-model="emailInput"
      label="Email"
      type="email"
      autocomplete="email"
      inputmode="email"
      :error="error"
    />
    <button
      class="btn btn-secondary btn-block"
      type="submit"
      :disabled="sending || cooldown > 0 || !emailInput.trim()"
    >
      {{ cooldown > 0 ? `Resend available in ${cooldown}s` : 'Resend confirmation email' }}
    </button>
    <p v-if="sent" class="muted small-note" role="status">
      If an unconfirmed account exists for this address, a new link is on its way.
    </p>
    <p v-if="error && email" class="field-error">{{ error }}</p>
  </form>
</template>
