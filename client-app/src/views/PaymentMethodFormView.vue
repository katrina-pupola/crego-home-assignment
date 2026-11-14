<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import FormField from '@/components/FormField.vue'
import MethodSummary from '@/components/MethodSummary.vue'
import MethodVisual from '@/components/MethodVisual.vue'
import * as api from '@/api/client'
import { toApiError } from '@/api/errors'
import type {
  MethodView,
  PaymentMethodRequest,
  PaymentMethodResponse,
  PaymentMethodType,
} from '@/api/types'
import {
  detectCardBrand,
  normalizeCardNumber,
  normalizeIban,
  validatePaymentMethod,
} from '@/utils/validation'

const router = useRouter()

const activeMethod = ref<PaymentMethodResponse | null>(null)
const hasPendingRequest = ref(false)
const loading = ref(true)

const type = ref<PaymentMethodType>('CARD')
const form = reactive({ cardNumber: '', cardHolder: '', cardExpiry: '', iban: '' })
const fieldErrors = ref<Record<string, string>>({})
const error = ref('')
const submitting = ref(false)

// The first method is added directly; any later one becomes a change request for BO approval
const isChange = computed(() => activeMethod.value !== null)

// Live preview of the method being entered, masked the same way the backend masks it
const preview = computed<MethodView>(() => {
  if (type.value === 'CARD') {
    const digits = normalizeCardNumber(form.cardNumber)
    const hasDigits = digits.length >= 4
    return {
      type: 'CARD',
      cardBrand: hasDigits ? detectCardBrand(digits) : null,
      cardHolder: form.cardHolder.trim() || null,
      cardExpiry: form.cardExpiry.trim() || null,
      maskedNumber: hasDigits ? `**** ${digits.slice(-4)}` : '',
    }
  }
  const iban = normalizeIban(form.iban)
  return {
    type: 'BANK_ACCOUNT',
    cardBrand: null,
    cardHolder: null,
    cardExpiry: null,
    maskedNumber: iban.length >= 6 ? `${iban.slice(0, 2)}** **** **** ${iban.slice(-4)}` : '',
  }
})

onMounted(async () => {
  try {
    const [method, requests] = await Promise.all([
      api.getActivePaymentMethod(),
      api.listChangeRequests(),
    ])
    activeMethod.value = method
    hasPendingRequest.value = requests.some((r) => r.status === 'PENDING')
  } catch (e) {
    error.value = toApiError(e).message
  } finally {
    loading.value = false
  }
})

function buildRequest(): PaymentMethodRequest {
  return type.value === 'CARD'
    ? {
        type: 'CARD',
        cardNumber: form.cardNumber,
        cardHolder: form.cardHolder,
        cardExpiry: form.cardExpiry,
      }
    : { type: 'BANK_ACCOUNT', iban: form.iban }
}

async function submit() {
  error.value = ''
  const request = buildRequest()
  fieldErrors.value = validatePaymentMethod(request)
  if (Object.keys(fieldErrors.value).length > 0) return

  submitting.value = true
  try {
    if (isChange.value) {
      await api.createChangeRequest(request)
    } else {
      await api.addFirstPaymentMethod(request)
    }
    await router.push('/')
  } catch (e) {
    const apiError = toApiError(e)
    error.value = apiError.message
    fieldErrors.value = apiError.fieldErrors
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <header class="page-header">
    <div>
      <p class="eyebrow">Payment method</p>
      <h1>{{ isChange ? 'Request a change' : 'Add payment method' }}</h1>
      <p class="muted">
        {{
          isChange
            ? 'Tell us which card or bank account you would like to use instead.'
            : 'Your first payment method is active as soon as you save it.'
        }}
      </p>
    </div>
  </header>

  <p v-if="loading" class="muted">Loading…</p>

  <section v-else-if="hasPendingRequest" class="card">
    <p>
      You already have a change request waiting for review. You can submit a new one once it is
      decided or cancelled.
    </p>
    <RouterLink class="btn btn-primary" to="/">Back to dashboard</RouterLink>
  </section>

  <div v-else class="form-grid">
    <section class="card">
      <p v-if="error" class="alert alert-error" role="alert">{{ error }}</p>

      <form novalidate @submit.prevent="submit">
        <fieldset class="type-tiles">
          <legend>What would you like to add?</legend>
          <label class="type-tile" :class="{ selected: type === 'CARD' }">
            <input v-model="type" class="visually-hidden" type="radio" value="CARD" />
            <span class="type-tile-icon" aria-hidden="true">💳</span>
            <span>
              <strong>Bank card</strong>
              <span class="muted">Visa, Mastercard, Amex</span>
            </span>
          </label>
          <label class="type-tile" :class="{ selected: type === 'BANK_ACCOUNT' }">
            <input v-model="type" class="visually-hidden" type="radio" value="BANK_ACCOUNT" />
            <span class="type-tile-icon" aria-hidden="true">🏦</span>
            <span>
              <strong>Bank account</strong>
              <span class="muted">Any IBAN</span>
            </span>
          </label>
        </fieldset>

        <template v-if="type === 'CARD'">
          <FormField
            id="cardNumber"
            v-model="form.cardNumber"
            label="Card number"
            inputmode="numeric"
            autocomplete="cc-number"
            placeholder="4111 1111 1111 1111"
            :error="fieldErrors.cardNumber"
          />
          <FormField
            id="cardHolder"
            v-model="form.cardHolder"
            label="Card holder"
            autocomplete="cc-name"
            placeholder="Name as shown on the card"
            :error="fieldErrors.cardHolder"
          />
          <FormField
            id="cardExpiry"
            v-model="form.cardExpiry"
            label="Expiry date"
            autocomplete="cc-exp"
            placeholder="MM/YY"
            :error="fieldErrors.cardExpiry"
          />
        </template>

        <FormField
          v-else
          id="iban"
          v-model="form.iban"
          label="IBAN"
          autocomplete="off"
          placeholder="LV80 BANK 0000 4351 9500 1"
          :error="fieldErrors.iban"
        />

        <div class="actions">
          <button class="btn btn-primary" type="submit" :disabled="submitting">
            {{
              submitting ? 'Submitting…' : isChange ? 'Submit for review' : 'Save payment method'
            }}
          </button>
          <RouterLink class="btn btn-link" to="/">Cancel</RouterLink>
        </div>
      </form>
    </section>

    <aside class="form-aside">
      <p class="section-label">Preview</p>
      <MethodVisual :method="preview" />

      <template v-if="activeMethod">
        <p class="section-label">Current method</p>
        <MethodSummary :method="activeMethod.method" />
        <p class="alert alert-info">
          Changes are reviewed by our team. Your current method stays active until the change is
          approved.
        </p>
      </template>
    </aside>
  </div>
</template>
