<!-- Large view of a payment method: card graphic for cards, details panel for bank accounts -->
<script setup lang="ts">
import type { MethodView } from '@/api/types'
import BankAccountPanel from './BankAccountPanel.vue'
import CardVisual from './CardVisual.vue'

defineProps<{ method: MethodView }>()
</script>

<template>
  <CardVisual v-if="method.type === 'CARD'" :method="method" />
  <BankAccountPanel v-else :method="method" />
</template>
