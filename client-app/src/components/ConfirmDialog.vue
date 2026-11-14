<!-- Confirmation modal built on the native <dialog>: dims the page, traps focus, closes on Esc -->
<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  open: boolean
  title: string
  message: string
  confirmLabel: string
  cancelLabel?: string
  danger?: boolean
  busy?: boolean
}>()

const emit = defineEmits<{ confirm: []; cancel: [] }>()

const dialog = ref<HTMLDialogElement | null>(null)

watch(
  () => props.open,
  (open) => {
    if (open && !dialog.value?.open) dialog.value?.showModal()
    if (!open && dialog.value?.open) dialog.value.close()
  },
)

// Esc key: let the parent decide, so its "open" state stays in sync
function onCancelEvent(event: Event) {
  event.preventDefault()
  if (!props.busy) emit('cancel')
}

// A click on the dimmed backdrop (outside the panel) counts as cancel
function onBackdropClick(event: MouseEvent) {
  if (event.target === dialog.value && !props.busy) emit('cancel')
}
</script>

<template>
  <dialog
    ref="dialog"
    class="dialog"
    aria-labelledby="dialog-title"
    @cancel="onCancelEvent"
    @click="onBackdropClick"
  >
    <div class="dialog-panel">
      <h2 id="dialog-title">{{ title }}</h2>
      <p class="muted">{{ message }}</p>
      <div class="dialog-actions">
        <button class="btn btn-secondary" type="button" :disabled="busy" @click="emit('cancel')">
          {{ cancelLabel ?? 'Cancel' }}
        </button>
        <button
          class="btn"
          :class="danger ? 'btn-danger' : 'btn-primary'"
          type="button"
          :disabled="busy"
          @click="emit('confirm')"
        >
          {{ busy ? 'Please wait…' : confirmLabel }}
        </button>
      </div>
    </div>
  </dialog>
</template>
