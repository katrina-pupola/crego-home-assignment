<!-- Compact one-line view of a payment method, for lists and "proposed method" rows -->
<script setup lang="ts">
import type { MethodView } from '@/api/types'
import { describeMethod } from '@/utils/format'

defineProps<{ method: MethodView }>()
</script>

<template>
  <div class="method">
    <span
      class="method-icon"
      :class="method.type === 'CARD' ? 'is-card' : 'is-bank'"
      aria-hidden="true"
    >
      {{ method.type === 'CARD' ? '💳' : '🏦' }}
    </span>
    <div>
      <p class="method-title">{{ describeMethod(method) }}</p>
      <p v-if="method.type === 'CARD'" class="muted">
        {{ method.cardHolder }} · expires {{ method.cardExpiry }}
      </p>
    </div>
  </div>
</template>
