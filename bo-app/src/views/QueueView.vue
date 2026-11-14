<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import StatusBadge from '@/components/StatusBadge.vue'
import * as api from '@/api/bo'
import { toApiError } from '@/api/errors'
import type { BoChangeRequestSummary, ChangeRequestStatus, PageResponse } from '@/api/types'
import { formatDate, typeLabels } from '@/utils/format'

type Tab = ChangeRequestStatus | 'ALL'

const REFRESH_INTERVAL_MS = 15_000
const tabs: { value: Tab; label: string }[] = [
  { value: 'PENDING', label: 'Pending' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'CANCELLED', label: 'Cancelled' },
  { value: 'ALL', label: 'All' },
]

const route = useRoute()
const router = useRouter()

const result = ref<PageResponse<BoChangeRequestSummary> | null>(null)
const loading = ref(true)
const error = ref('')

// Tab and page live in the URL, so a refresh or the back button keeps them
const activeTab = computed<Tab>(() => {
  const status = route.query.status
  return tabs.some((t) => t.value === status) ? (status as Tab) : 'PENDING'
})
const page = computed(() => Math.max(0, Number(route.query.page) || 0))

// Shown after returning from a decision, e.g. "?decided=APPROVED&id=12"
const flash = computed(() => {
  const { decided, id } = route.query
  if (decided === 'APPROVED') return { type: 'success', text: `Request #${id} was approved.` }
  if (decided === 'REJECTED') return { type: 'info', text: `Request #${id} was rejected.` }
  return null
})

async function load(showSpinner = false) {
  if (showSpinner) loading.value = true
  try {
    const status = activeTab.value === 'ALL' ? null : activeTab.value
    result.value = await api.listRequests(status, page.value)
    error.value = ''
  } catch (e) {
    error.value = toApiError(e).message
  } finally {
    loading.value = false
  }
}

watch([activeTab, page], () => load(true), { immediate: true })

function selectTab(tab: Tab) {
  router.push({ query: { status: tab } })
}

function goToPage(target: number) {
  router.push({ query: { status: activeTab.value, page: String(target) } })
}

function openRequest(id: number) {
  router.push({ name: 'request', params: { id } })
}

function dismissFlash() {
  router.replace({ query: { status: activeTab.value, page: route.query.page } })
}

// Refresh quietly so new client requests appear without reloading the page
let timer: number | undefined
onMounted(() => {
  timer = window.setInterval(() => load(), REFRESH_INTERVAL_MS)
})
onUnmounted(() => window.clearInterval(timer))
</script>

<template>
  <header class="page-header">
    <div>
      <p class="eyebrow">Back Office</p>
      <h1>Change requests</h1>
      <p class="muted">Review the payment method changes requested by clients.</p>
    </div>
    <span v-if="activeTab === 'PENDING' && result" class="badge badge-pending">
      {{ result.totalElements }} waiting
    </span>
  </header>

  <div
    v-if="flash"
    class="alert"
    :class="flash.type === 'success' ? 'alert-success' : 'alert-info'"
    role="status"
  >
    <span>{{ flash.text }}</span>
    <button class="btn btn-link" type="button" @click="dismissFlash">Dismiss</button>
  </div>

  <nav class="tabs" aria-label="Filter by status">
    <button
      v-for="tab in tabs"
      :key="tab.value"
      class="tab"
      :class="{ active: tab.value === activeTab }"
      type="button"
      :aria-current="tab.value === activeTab ? 'page' : undefined"
      @click="selectTab(tab.value)"
    >
      {{ tab.label }}
    </button>
  </nav>

  <p v-if="loading" class="muted">Loading…</p>
  <p v-else-if="error" class="alert alert-error" role="alert">{{ error }}</p>

  <section v-else-if="result && result.content.length === 0" class="card empty-state">
    <span class="empty-icon" aria-hidden="true">✅</span>
    <h2>{{ activeTab === 'PENDING' ? 'All caught up' : 'Nothing here yet' }}</h2>
    <p class="muted">
      {{
        activeTab === 'PENDING'
          ? 'There are no requests waiting for review. New ones appear here automatically.'
          : 'No requests match this filter.'
      }}
    </p>
  </section>

  <div v-else-if="result" class="card table-wrap">
    <table>
      <thead>
        <tr>
          <th>Request</th>
          <th>Client</th>
          <th>Proposed method</th>
          <th>Submitted</th>
          <th>Status</th>
          <th><span class="visually-hidden">Action</span></th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="request in result.content"
          :key="request.id"
          class="row-link"
          @click="openRequest(request.id)"
        >
          <td class="cell-id">#{{ request.id }}</td>
          <td class="cell-client">
            <strong>{{ request.clientName }}</strong>
            <span class="muted">{{ request.clientEmail }}</span>
          </td>
          <td class="cell-method">
            <span aria-hidden="true">{{ request.proposedType === 'CARD' ? '💳' : '🏦' }}</span>
            {{ typeLabels[request.proposedType] }}
          </td>
          <td class="cell-date" data-label="Submitted">{{ formatDate(request.createdAt) }}</td>
          <td class="cell-status"><StatusBadge :status="request.status" /></td>
          <td class="cell-action">
            <RouterLink
              class="btn btn-link"
              :to="{ name: 'request', params: { id: request.id } }"
              @click.stop
            >
              {{ request.status === 'PENDING' ? 'Review →' : 'View →' }}
            </RouterLink>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="result.totalPages > 1" class="pagination">
      <span class="muted">
        Page {{ result.page + 1 }} of {{ result.totalPages }} · {{ result.totalElements }} requests
      </span>
      <div class="actions">
        <button
          class="btn btn-secondary"
          type="button"
          :disabled="result.page === 0"
          @click="goToPage(result.page - 1)"
        >
          Previous
        </button>
        <button
          class="btn btn-secondary"
          type="button"
          :disabled="result.page + 1 >= result.totalPages"
          @click="goToPage(result.page + 1)"
        >
          Next
        </button>
      </div>
    </div>
  </div>
</template>
