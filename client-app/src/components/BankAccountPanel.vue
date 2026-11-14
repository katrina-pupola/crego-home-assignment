<!-- Bank account shown as a flat details panel, deliberately not card-shaped -->
<script setup lang="ts">
import { computed } from 'vue'
import type { MethodView } from '@/api/types'
import { countryName } from '@/utils/format'

const props = defineProps<{ method: MethodView }>()

// "LV** **** **** 5001" -> "LV•• •••• •••• 5001"
const displayIban = computed(() =>
  props.method.maskedNumber ? props.method.maskedNumber.replace(/\*/g, '•') : '—',
)

const country = computed(() => {
  const code = props.method.maskedNumber.slice(0, 2)
  return /^[A-Z]{2}$/.test(code) ? countryName(code) : '—'
})
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
      <div>
        <dt>IBAN</dt>
        <dd class="mono">{{ displayIban }}</dd>
      </div>
      <div>
        <dt>Country</dt>
        <dd>{{ country }}</dd>
      </div>
    </dl>
  </div>
</template>
