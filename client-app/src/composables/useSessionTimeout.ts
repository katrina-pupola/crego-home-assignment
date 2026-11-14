import { computed, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

export const WARNING_BEFORE_MS = 2 * 60 * 1000

/**
 * Warns the user 2 minutes before the token expires and logs them out exactly at expiry,
 * so an idle page never keeps looking logged in after the session has ended.
 */
export function useSessionTimeout() {
  const auth = useAuthStore()
  const router = useRouter()

  const showWarning = ref(false)
  const secondsLeft = ref(0)
  let warningTimer: ReturnType<typeof setTimeout> | undefined
  let expiryTimer: ReturnType<typeof setTimeout> | undefined
  let countdownTimer: ReturnType<typeof setInterval> | undefined

  // "1:05" for the countdown in the warning dialog
  const timeLeft = computed(() => {
    const minutes = Math.floor(secondsLeft.value / 60)
    const seconds = String(secondsLeft.value % 60).padStart(2, '0')
    return `${minutes}:${seconds}`
  })

  function clearTimers() {
    clearTimeout(warningTimer)
    clearTimeout(expiryTimer)
    clearInterval(countdownTimer)
    showWarning.value = false
  }

  function goToLogin(expired: boolean) {
    const current = router.currentRoute.value.fullPath
    clearTimers()
    auth.logout()
    router.push({
      name: 'login',
      query: { redirect: current, ...(expired ? { expired: '1' } : {}) },
    })
  }

  function startWarning() {
    const update = () => {
      secondsLeft.value = Math.max(0, Math.round(((auth.expiresAt ?? 0) - Date.now()) / 1000))
    }
    update()
    showWarning.value = true
    countdownTimer = setInterval(update, 1000)
  }

  // Re-plan the timers whenever the user logs in, logs out or gets a new token
  watch(
    () => auth.expiresAt,
    (expiresAt) => {
      clearTimers()
      if (expiresAt === null) return
      const msLeft = expiresAt - Date.now()
      if (msLeft <= 0) return // already expired: the route guard or the next API call handles it

      expiryTimer = setTimeout(() => goToLogin(true), msLeft)
      const msUntilWarning = msLeft - WARNING_BEFORE_MS
      if (msUntilWarning > 0) {
        warningTimer = setTimeout(startWarning, msUntilWarning)
      } else {
        startWarning()
      }
    },
    { immediate: true },
  )

  onUnmounted(clearTimers)

  function dismissWarning() {
    clearInterval(countdownTimer)
    showWarning.value = false
  }

  // Without refresh tokens a session can't be extended; logging in again starts a new one
  function logInAgain() {
    goToLogin(false)
  }

  return { showWarning, timeLeft, dismissWarning, logInAgain }
}
