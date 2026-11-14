<!-- Bank card drawn like the physical card, coloured by brand -->
<script setup lang="ts">
import { computed } from 'vue'
import type { MethodView } from '@/api/types'
import { brandName } from '@/utils/format'

const props = defineProps<{ method: MethodView }>()

const brandClass = computed(() => `pcard-${(props.method.cardBrand ?? 'OTHER').toLowerCase()}`)

// "**** 1111" -> "•••• •••• •••• 1111"
const displayNumber = computed(
  () => `•••• •••• •••• ${props.method.maskedNumber.slice(-4) || '••••'}`,
)
</script>

<template>
  <div class="pcard" :class="brandClass">
    <div class="pcard-top">
      <span class="pcard-chip" aria-hidden="true"></span>
      <span class="pcard-brand">
        <span v-if="method.cardBrand === 'MASTERCARD'" class="mc-logo" aria-label="Mastercard">
          <span></span><span></span>
        </span>
        <template v-else>{{ brandName(method.cardBrand) }}</template>
      </span>
    </div>

    <p class="pcard-number">{{ displayNumber }}</p>

    <div class="pcard-bottom">
      <div>
        <span class="pcard-label">Card holder</span>
        <span class="pcard-value">{{ method.cardHolder || 'Your name' }}</span>
      </div>
      <div>
        <span class="pcard-label">Expires</span>
        <span class="pcard-value">{{ method.cardExpiry || 'MM/YY' }}</span>
      </div>
    </div>
  </div>
</template>
