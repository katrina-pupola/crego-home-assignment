<!-- Three-step progress of a change request: submitted, in review, decision -->
<script setup lang="ts">
import { computed } from 'vue'
import type { ChangeRequestStatus } from '@/api/types'
import { formatDate } from '@/utils/format'

const props = defineProps<{
  status: ChangeRequestStatus
  createdAt: string
  reviewedAt: string | null
}>()

const finalLabels: Record<ChangeRequestStatus, string> = {
  PENDING: 'Decision',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  CANCELLED: 'Cancelled by client',
}

const steps = computed(() => {
  const pending = props.status === 'PENDING'
  return [
    { label: 'Submitted', detail: formatDate(props.createdAt), state: 'done' },
    {
      label: 'In review',
      detail: pending ? 'Waiting for a decision' : '',
      state: pending ? 'current' : props.status === 'CANCELLED' ? 'skipped' : 'done',
    },
    {
      label: finalLabels[props.status],
      detail: props.reviewedAt ? formatDate(props.reviewedAt) : '',
      state: pending ? 'upcoming' : props.status.toLowerCase(),
    },
  ]
})
</script>

<template>
  <ol class="timeline">
    <li v-for="step in steps" :key="step.label" class="timeline-step" :class="`is-${step.state}`">
      <span class="timeline-dot" aria-hidden="true"></span>
      <span class="timeline-label">{{ step.label }}</span>
      <span v-if="step.detail" class="timeline-detail">{{ step.detail }}</span>
    </li>
  </ol>
</template>
