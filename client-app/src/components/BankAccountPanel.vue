<!-- Bank account shown as a flat details panel, deliberately not card-shaped -->
<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import type { MethodView } from '@/api/types'
import { countryName, groupIban } from '@/utils/format'

const props = defineProps<{
  method: MethodView
  /** Full IBAN, only passed for the client's own active account; enables Show and Copy */
  fullIban?: string | null
}>()

const revealed = ref(false)
const copyStatus = ref<'' | 'copied' | 'failed'>('')
let copyTimer: ReturnType<typeof setTimeout> | undefined

// Masked by default ("LV•• •••• •••• 5001"); grouped in fours when revealed
const displayIban = computed(() => {
  if (revealed.value && props.fullIban) return groupIban(props.fullIban)
  return props.method.maskedNumber ? props.method.maskedNumber.replace(/\*/g, '•') : '—'
})

const country = computed(() => {
  const code = props.method.maskedNumber.slice(0, 2)
  return /^[A-Z]{2}$/.test(code) ? countryName(code) : '—'
})

async function copyIban() {
  if (!props.fullIban) return
  try {
    await navigator.clipboard.writeText(props.fullIban)
    copyStatus.value = 'copied'
  } catch {
    copyStatus.value = 'failed' // e.g. clipboard blocked by the browser
  }
  clearTimeout(copyTimer)
  copyTimer = setTimeout(() => (copyStatus.value = ''), 2000)
}

onUnmounted(() => clearTimeout(copyTimer))
</script>

<template>
  <div class="bank-panel">
    <div class="bank-panel-header">
      <span class="bank-panel-icon" aria-hidden="true">🏦</span>
      <div>
        <p class="bank-panel-title">Bank account</p>
        <p class="muted">Payments by bank transfer</p>
      </div>
    </div>
    <dl class="bank-panel-details">
      <div class="stacked">
        <dt>IBAN</dt>
        <dd class="mono">{{ displayIban }}</dd>
      </div>
      <div>
        <dt>Country</dt>
        <dd>{{ country }}</dd>
      </div>
    </dl>
    <div v-if="fullIban" class="bank-panel-actions">
      <button
        class="btn btn-link"
        type="button"
        :aria-pressed="revealed"
        @click="revealed = !revealed"
      >
        {{ revealed ? 'Hide IBAN' : 'Show IBAN' }}
      </button>
      <button class="btn btn-link" type="button" @click="copyIban">Copy IBAN</button>
      <span class="muted bank-panel-status" role="status">
        {{ copyStatus === 'copied' ? 'Copied' : copyStatus === 'failed' ? 'Could not copy' : '' }}
      </span>
    </div>
  </div>
</template>
