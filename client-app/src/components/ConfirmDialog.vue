<!-- Confirmation modal built on the native <dialog>: dims the page, traps focus, closes on Esc -->
<script setup lang="ts">
import { ref, useId, watch } from 'vue'

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
// Unique per dialog, so screen readers announce the right title when several dialogs exist
const titleId = useId()

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

// Closes only when both press and release happen on the dimmed backdrop, so dragging a
// text selection out of a field does not close the dialog
let pressedOnBackdrop = false
function onMouseDown(event: MouseEvent) {
  pressedOnBackdrop = event.target === dialog.value
}
function onBackdropClick(event: MouseEvent) {
  if (pressedOnBackdrop && event.target === dialog.value && !props.busy) emit('cancel')
  pressedOnBackdrop = false
}
</script>

<template>
  <dialog
    ref="dialog"
    class="dialog"
    :aria-labelledby="titleId"
    @cancel="onCancelEvent"
    @mousedown="onMouseDown"
    @click="onBackdropClick"
  >
    <div class="dialog-panel">
      <h2 :id="titleId">{{ title }}</h2>
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
