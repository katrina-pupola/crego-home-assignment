<!-- Large view of a payment method: card graphic for cards, details panel for bank accounts -->
<script setup lang="ts">
import type { MethodView } from '@/api/types'
import BankAccountPanel from './BankAccountPanel.vue'
import CardVisual from './CardVisual.vue'

defineProps<{
  method: MethodView
  /** Full IBAN of the client's own account (dashboard only) */
  fullIban?: string | null
}>()
</script>

<template>
  <CardVisual v-if="method.type === 'CARD'" :method="method" />
  <BankAccountPanel v-else :method="method" :full-iban="fullIban" />
</template>
