<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import MethodVisual from '@/components/MethodVisual.vue'
import RequestTimeline from '@/components/RequestTimeline.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import * as api from '@/api/bo'
import { toApiError } from '@/api/errors'
import type { BoChangeRequestDetail, MethodView } from '@/api/types'
import { brandName, displayNumber, formatDate, typeLabels } from '@/utils/format'

const MAX_COMMENT_LENGTH = 1000

const props = defineProps<{ id: number }>()
const router = useRouter()

const request = ref<BoChangeRequestDetail | null>(null)
const loading = ref(true)
const error = ref('')

const isPending = computed(() => request.value?.status === 'PENDING')

const labels = computed(() => {
  switch (request.value?.status) {
    case 'APPROVED':
      return { before: 'Previous method', after: 'New method' }
    case 'REJECTED':
    case 'CANCELLED':
      return { before: 'Method at request time', after: 'Proposed (not applied)' }
    default:
      return { before: 'Current method', after: 'Proposed method' }
  }
})

// Field-by-field list of what the change would modify, so the reviewer sees it at a glance
const changes = computed(() => {
  const before = request.value?.currentMethod
  const after = request.value?.proposedMethod
  if (!before || !after) return []

  const describe = (method: MethodView) => ({
    Type: typeLabels[method.type],
    Number: displayNumber(method),
    Brand: method.type === 'CARD' ? brandName(method.cardBrand) : '—',
    'Card holder': method.cardHolder ?? '—',
    Expiry: method.cardExpiry ?? '—',
  })
  const from = describe(before)
  const to = describe(after)
  return (Object.keys(from) as (keyof typeof from)[])
    .filter((field) => from[field] !== to[field])
    .map((field) => ({ field, from: from[field], to: to[field] }))
})

async function load() {
  try {
    request.value = await api.getRequest(props.id)
  } catch (e) {
    error.value = toApiError(e).message
  } finally {
    loading.value = false
  }
}

onMounted(load)

// Approve and reject both confirm in a modal first
const dialog = ref<'approve' | 'reject' | null>(null)
const comment = ref('')
const reason = ref('')
const reasonError = ref('')
const deciding = ref(false)

function openDialog(kind: 'approve' | 'reject') {
  comment.value = ''
  reason.value = ''
  reasonError.value = ''
  dialog.value = kind
}

async function decide() {
  const kind = dialog.value
  if (!kind) return
  deciding.value = true
  error.value = ''
  try {
    if (kind === 'approve') {
      await api.approve(props.id, comment.value)
    } else {
      await api.reject(props.id, reason.value)
    }
    await router.push({
      name: 'queue',
      query: { decided: kind === 'approve' ? 'APPROVED' : 'REJECTED', id: String(props.id) },
    })
  } catch (e) {
    const apiError = toApiError(e)
    if (kind === 'reject' && apiError.fieldErrors.reason) {
      reasonError.value = apiError.fieldErrors.reason
      return
    }
    dialog.value = null
    // 409: someone else decided first, or the client cancelled; show the latest state
    error.value =
      apiError.status === 409
        ? `${apiError.message}. The latest state is shown below.`
        : apiError.message
    await load()
  } finally {
    deciding.value = false
  }
}
</script>

<template>
  <RouterLink class="back-link" to="/">← Change requests</RouterLink>

  <p v-if="loading" class="muted">Loading…</p>
  <p v-else-if="!request" class="alert alert-error" role="alert">
    {{ error || 'Request not found.' }}
  </p>

  <template v-else>
    <header class="page-header">
      <div>
        <p class="eyebrow">Change request</p>
        <div class="title-row">
          <h1>Request #{{ request.id }}</h1>
          <StatusBadge :status="request.status" />
        </div>
        <p class="muted">
          Submitted {{ formatDate(request.createdAt) }} by {{ request.client.fullName }}
        </p>
      </div>
    </header>

    <p v-if="error" class="alert alert-error" role="alert">{{ error }}</p>

    <div class="detail-grid">
      <div>
        <section class="card">
          <div class="card-header">
            <h2>Requested change</h2>
          </div>
          <div class="comparison">
            <div>
              <p class="section-label">{{ labels.before }}</p>
              <MethodVisual v-if="request.currentMethod" :method="request.currentMethod" />
              <p v-else class="muted">No method</p>
            </div>
            <span class="comparison-arrow" aria-hidden="true">→</span>
            <div>
              <p class="section-label">{{ labels.after }}</p>
              <MethodVisual :method="request.proposedMethod" />
            </div>
          </div>

          <ul v-if="changes.length" class="changes" aria-label="What changes">
            <li v-for="change in changes" :key="change.field">
              <span class="change-field">{{ change.field }}</span>
              <span class="change-values">
                <span class="old">{{ change.from }}</span>
                <span class="change-arrow" aria-label="changes to">→</span>
                <span class="new">{{ change.to }}</span>
              </span>
            </li>
          </ul>
        </section>

        <section class="card">
          <div class="card-header">
            <h2>Progress</h2>
          </div>
          <RequestTimeline
            :status="request.status"
            :created-at="request.createdAt"
            :reviewed-at="request.reviewedAt"
          />
        </section>
      </div>

      <div>
        <section class="card client-info">
          <div class="card-header">
            <h2>Client</h2>
          </div>
          <dl>
            <div>
              <dt>Name</dt>
              <dd>{{ request.client.fullName }}</dd>
            </div>
            <div>
              <dt>Email</dt>
              <dd>{{ request.client.email }}</dd>
            </div>
          </dl>
        </section>

        <section class="card">
          <div class="card-header">
            <h2>Decision</h2>
          </div>

          <template v-if="isPending">
            <p class="muted">
              Approving makes the proposed method the client's active payment method right away.
            </p>
            <div class="decision-actions">
              <button class="btn btn-danger" type="button" @click="openDialog('reject')">
                Reject
              </button>
              <button class="btn btn-success" type="button" @click="openDialog('approve')">
                Approve
              </button>
            </div>
          </template>

          <p v-else-if="request.status === 'CANCELLED'" class="muted">
            The client cancelled this request before it was reviewed.
          </p>

          <template v-else>
            <p>
              <strong>{{ request.status === 'APPROVED' ? 'Approved' : 'Rejected' }}</strong>
              by {{ request.reviewerName }}
              <span class="muted">· {{ formatDate(request.reviewedAt) }}</span>
            </p>
            <blockquote v-if="request.reviewComment" class="review-comment">
              <span class="section-label">
                {{ request.status === 'REJECTED' ? 'Reason' : 'Comment' }}
              </span>
              {{ request.reviewComment }}
            </blockquote>
          </template>
        </section>
      </div>
    </div>

    <ConfirmDialog
      :open="dialog === 'approve'"
      title="Approve this change?"
      message="The proposed method becomes the client's active payment method immediately."
      confirm-label="Approve"
      :busy="deciding"
      @confirm="decide"
      @cancel="dialog = null"
    >
      <div class="field">
        <label for="comment">Comment for the client (optional)</label>
        <textarea id="comment" v-model="comment" :maxlength="MAX_COMMENT_LENGTH"></textarea>
      </div>
    </ConfirmDialog>

    <ConfirmDialog
      :open="dialog === 'reject'"
      title="Reject this change?"
      message="The client keeps their current method and will see your reason."
      confirm-label="Reject"
      danger
      :busy="deciding"
      :confirm-disabled="!reason.trim()"
      @confirm="decide"
      @cancel="dialog = null"
    >
      <div class="field">
        <label for="reason">Reason (required)</label>
        <textarea
          id="reason"
          v-model="reason"
          :maxlength="MAX_COMMENT_LENGTH"
          :aria-invalid="reasonError ? 'true' : undefined"
          aria-describedby="reason-hint"
        ></textarea>
        <p v-if="reasonError" class="field-error">{{ reasonError }}</p>
        <p id="reason-hint" class="field-hint">{{ reason.length }} / {{ MAX_COMMENT_LENGTH }}</p>
      </div>
    </ConfirmDialog>
  </template>
</template>
