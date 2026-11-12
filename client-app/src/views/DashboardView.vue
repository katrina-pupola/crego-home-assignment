<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import MethodSummary from '@/components/MethodSummary.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import * as api from '@/api/client'
import { toApiError } from '@/api/errors'
import type { ChangeRequestResponse, PaymentMethodResponse } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { formatDate } from '@/utils/format'

const POLL_INTERVAL_MS = 10_000

const auth = useAuthStore()

const activeMethod = ref<PaymentMethodResponse | null>(null)
const latestRequest = ref<ChangeRequestResponse | null>(null)
const loading = ref(true)
const error = ref('')
const cancelling = ref(false)

const isPending = computed(() => latestRequest.value?.status === 'PENDING')

// The decision banner stays until dismissed; the dismissed request id is remembered per user
const seenKey = computed(() => `crego.client.seenDecision.${auth.userId}`)
const seenDecisionId = ref(localStorage.getItem(seenKey.value))
const unseenDecision = computed(() => {
  const request = latestRequest.value
  if (!request || (request.status !== 'APPROVED' && request.status !== 'REJECTED')) return null
  return String(request.id) !== seenDecisionId.value ? request : null
})

function dismissDecision() {
  if (!unseenDecision.value) return
  seenDecisionId.value = String(unseenDecision.value.id)
  localStorage.setItem(seenKey.value, seenDecisionId.value)
}

async function load() {
  try {
    const [method, requests] = await Promise.all([
      api.getActivePaymentMethod(),
      api.listChangeRequests(),
    ])
    activeMethod.value = method
    latestRequest.value = requests[0] ?? null
    error.value = ''
  } catch (e) {
    error.value = toApiError(e).message
  } finally {
    loading.value = false
  }
}

async function cancelRequest() {
  if (!latestRequest.value || !confirm('Cancel this change request?')) return
  cancelling.value = true
  try {
    await api.cancelChangeRequest(latestRequest.value.id)
    await load()
  } catch (e) {
    error.value = toApiError(e).message
  } finally {
    cancelling.value = false
  }
}

// Poll only while a request waits for a decision
let timer: number | undefined
function stopPolling() {
  window.clearInterval(timer)
  timer = undefined
}
watch(isPending, (pending) => {
  if (pending && timer === undefined) timer = window.setInterval(load, POLL_INTERVAL_MS)
  if (!pending) stopPolling()
})

onMounted(load)
onUnmounted(stopPolling)
</script>

<template>
  <h1>Dashboard</h1>

  <p v-if="loading" class="muted">Loading…</p>
  <p v-else-if="error" class="alert alert-error" role="alert">{{ error }}</p>

  <template v-else>
    <div
      v-if="unseenDecision"
      class="alert"
      :class="unseenDecision.status === 'APPROVED' ? 'alert-success' : 'alert-error'"
      role="status"
    >
      <div>
        <strong v-if="unseenDecision.status === 'APPROVED'"
          >Your change request was approved.</strong
        >
        <strong v-else>Your change request was rejected.</strong>
        <p v-if="unseenDecision.reviewComment">
          Reviewer's comment: {{ unseenDecision.reviewComment }}
        </p>
      </div>
      <button class="btn btn-link" type="button" @click="dismissDecision">Dismiss</button>
    </div>

    <section class="card">
      <h2>Current payment method</h2>
      <template v-if="activeMethod">
        <MethodSummary :method="activeMethod.method" />
        <p class="muted">Active since {{ formatDate(activeMethod.createdAt) }}</p>
        <RouterLink v-if="!isPending" class="btn btn-primary" to="/payment-method/new"
          >Request a change</RouterLink
        >
      </template>
      <template v-else>
        <p>You have not added a payment method yet.</p>
        <RouterLink class="btn btn-primary" to="/payment-method/new">Add payment method</RouterLink>
      </template>
    </section>

    <section v-if="latestRequest" class="card">
      <div class="card-header">
        <h2>Latest change request</h2>
        <StatusBadge :status="latestRequest.status" />
      </div>
      <p class="muted">Proposed method</p>
      <MethodSummary :method="latestRequest.proposedMethod" />
      <p class="muted">
        Submitted {{ formatDate(latestRequest.createdAt) }}
        <template v-if="latestRequest.reviewedAt">
          · decided {{ formatDate(latestRequest.reviewedAt) }}</template
        >
      </p>
      <p v-if="latestRequest.reviewComment">
        Reviewer's comment: {{ latestRequest.reviewComment }}
      </p>

      <template v-if="isPending">
        <p class="muted">Our team is reviewing your request. This page updates automatically.</p>
        <button
          class="btn btn-secondary"
          type="button"
          :disabled="cancelling"
          @click="cancelRequest"
        >
          {{ cancelling ? 'Cancelling…' : 'Cancel request' }}
        </button>
      </template>
      <RouterLink class="btn btn-link" to="/requests">View all requests</RouterLink>
    </section>
  </template>
</template>
