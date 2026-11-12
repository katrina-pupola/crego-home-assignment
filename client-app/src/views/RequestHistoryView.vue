<script setup lang="ts">
import { onMounted, ref } from 'vue'
import StatusBadge from '@/components/StatusBadge.vue'
import * as api from '@/api/client'
import { toApiError } from '@/api/errors'
import type { ChangeRequestResponse } from '@/api/types'
import { describeMethod, formatDate } from '@/utils/format'

const requests = ref<ChangeRequestResponse[]>([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    requests.value = await api.listChangeRequests()
  } catch (e) {
    error.value = toApiError(e).message
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <h1>Request history</h1>

  <p v-if="loading" class="muted">Loading…</p>
  <p v-else-if="error" class="alert alert-error" role="alert">{{ error }}</p>
  <p v-else-if="requests.length === 0" class="card">You have not requested any changes yet.</p>

  <div v-else class="card table-wrap">
    <table>
      <thead>
        <tr>
          <th>Submitted</th>
          <th>Proposed method</th>
          <th>Status</th>
          <th>Decided</th>
          <th>Reviewer's comment</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="request in requests" :key="request.id">
          <td>{{ formatDate(request.createdAt) }}</td>
          <td>{{ describeMethod(request.proposedMethod) }}</td>
          <td><StatusBadge :status="request.status" /></td>
          <td>{{ formatDate(request.reviewedAt) }}</td>
          <td>{{ request.reviewComment ?? '—' }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
