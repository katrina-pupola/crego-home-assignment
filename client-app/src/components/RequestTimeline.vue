<!-- Three-step progress of a change request: submitted, in review, decision -->
<script setup lang="ts">
import { computed } from 'vue'
import type { ChangeRequestResponse, ChangeRequestStatus } from '@/api/types'
import { formatDate } from '@/utils/format'

const props = defineProps<{ request: ChangeRequestResponse }>()

const finalLabels: Record<ChangeRequestStatus, string> = {
  PENDING: 'Decision',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  CANCELLED: 'Cancelled',
}

const steps = computed(() => {
  const { status, createdAt, reviewedAt } = props.request
  const pending = status === 'PENDING'
  return [
    { label: 'Submitted', detail: formatDate(createdAt), state: 'done' },
    {
      label: 'In review',
      detail: pending ? 'Waiting for our team' : '',
      state: pending ? 'current' : status === 'CANCELLED' ? 'skipped' : 'done',
    },
    {
      label: finalLabels[status],
      detail: reviewedAt ? formatDate(reviewedAt) : '',
      state: pending ? 'upcoming' : status.toLowerCase(),
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
