<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import MethodSummary from '@/components/MethodSummary.vue'
import MethodVisual from '@/components/MethodVisual.vue'
import RequestTimeline from '@/components/RequestTimeline.vue'
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
const firstName = computed(() => auth.profile?.fullName.split(' ')[0] ?? '')

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

const showCancelDialog = ref(false)
const cancelError = ref('')

async function cancelRequest() {
  if (!latestRequest.value) return
  cancelling.value = true
  cancelError.value = ''
  try {
    await api.cancelChangeRequest(latestRequest.value.id)
    await load()
  } catch (e) {
    // e.g. staff decided a moment earlier: show why next to the request and load its new state
    cancelError.value = toApiError(e).message
    await load()
  } finally {
    cancelling.value = false
    showCancelDialog.value = false
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
  <header class="page-header">
    <div>
      <p class="eyebrow">Dashboard</p>
      <h1>Hello{{ firstName ? `, ${firstName}` : '' }}</h1>
      <p class="muted">Your payment method and the status of your latest request.</p>
    </div>
    <RouterLink
      v-if="!loading && activeMethod && !isPending"
      class="btn btn-primary"
      to="/payment-method/new"
    >
      Request a change
    </RouterLink>
  </header>

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

    <!-- No method yet: one clear call to action -->
    <section v-if="!activeMethod" class="card empty-state">
      <span class="empty-icon" aria-hidden="true">💳</span>
      <h2>Add your first payment method</h2>
      <p class="muted">
        Add a bank card or a bank account. Your first method is active right away.
      </p>
      <RouterLink class="btn btn-primary" to="/payment-method/new">Add payment method</RouterLink>
    </section>

    <div v-else class="dashboard-grid">
      <section class="card">
        <div class="card-header">
          <h2>Current payment method</h2>
          <span class="pill pill-active">Active</span>
        </div>
        <MethodVisual :method="activeMethod.method" />
        <p class="muted card-footnote">Active since {{ formatDate(activeMethod.createdAt) }}</p>
      </section>

      <section class="card">
        <div class="card-header">
          <h2>Latest change request</h2>
          <StatusBadge v-if="latestRequest" :status="latestRequest.status" />
        </div>

        <template v-if="latestRequest">
          <RequestTimeline :request="latestRequest" />

          <p class="section-label">Proposed method</p>
          <MethodSummary :method="latestRequest.proposedMethod" />

          <blockquote v-if="latestRequest.reviewComment" class="review-comment">
            <span class="section-label">Reviewer's comment</span>
            {{ latestRequest.reviewComment }}
          </blockquote>

          <p v-if="isPending" class="muted">
            Our team is reviewing your request. This page updates automatically.
          </p>

          <p v-if="cancelError" class="alert alert-error" role="alert">{{ cancelError }}</p>

          <div class="actions">
            <button
              v-if="isPending"
              class="btn btn-secondary"
              type="button"
              :disabled="cancelling"
              @click="showCancelDialog = true"
            >
              {{ cancelling ? 'Cancelling…' : 'Cancel request' }}
            </button>
            <RouterLink class="btn btn-link" to="/requests">View all requests</RouterLink>
          </div>
        </template>

        <div v-else class="empty-inline">
          <p class="muted">
            No change requests yet. When you need to switch to another card or bank account, request
            a change and our team will review it.
          </p>
        </div>
      </section>
    </div>
  </template>

  <ConfirmDialog
    :open="showCancelDialog"
    title="Cancel this change request?"
    message="Your current payment method stays active. You can submit a new request at any time."
    confirm-label="Cancel request"
    cancel-label="Keep request"
    danger
    :busy="cancelling"
    @confirm="cancelRequest"
    @cancel="showCancelDialog = false"
  />
</template>
