<script setup lang="ts">
import type { ChangeRequestStatus } from '@/api/types'
import { statusLabels } from '@/utils/format'

defineProps<{ status: ChangeRequestStatus }>()
</script>

<template>
  <span class="badge" :class="`badge-${status.toLowerCase()}`">{{ statusLabels[status] }}</span>
</template>
