import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter, type Router } from 'vue-router'
import { useSessionTimeout, WARNING_BEFORE_MS } from '../useSessionTimeout'
import { useAuthStore } from '@/stores/auth'

const SESSION_MS = 60 * 60 * 1000

// In-memory localStorage; newer Node versions ship their own global that hides jsdom's
function memoryStorage(): Storage {
  const data = new Map<string, string>()
  return {
    get length() {
      return data.size
    },
    clear: () => data.clear(),
    getItem: (key) => data.get(key) ?? null,
    key: (index) => [...data.keys()][index] ?? null,
    removeItem: (key) => void data.delete(key),
    setItem: (key, value) => void data.set(key, String(value)),
  }
}

// Unsigned token with only the claims the frontend reads
function fakeToken(expiresInMs: number): string {
  const exp = Math.floor((Date.now() + expiresInMs) / 1000)
  return `header.${btoa(JSON.stringify({ sub: '1', exp }))}.signature`
}

async function setup() {
  const pinia = createPinia()
  const router: Router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: { render: () => null } },
      { path: '/requests', name: 'requests', component: { render: () => null } },
      { path: '/login', name: 'login', component: { render: () => null } },
    ],
  })
  await router.push('/requests')

  let session!: ReturnType<typeof useSessionTimeout>
  const Host = defineComponent({
    setup() {
      session = useSessionTimeout()
      return () => null
    },
  })
  mount(Host, { global: { plugins: [pinia, router] } })
  return { session, router, auth: useAuthStore(pinia) }
}

describe('useSessionTimeout', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 10, 14, 12, 0, 0))
    vi.stubGlobal('localStorage', memoryStorage())
    localStorage.setItem('crego.client.token', fakeToken(SESSION_MS))
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('warns two minutes before the session expires', async () => {
    const { session } = await setup()

    vi.advanceTimersByTime(SESSION_MS - WARNING_BEFORE_MS - 1000)
    expect(session.showWarning.value).toBe(false)

    vi.advanceTimersByTime(1000)
    expect(session.showWarning.value).toBe(true)
    expect(session.timeLeft.value).toBe('2:00')

    vi.advanceTimersByTime(65_000)
    expect(session.timeLeft.value).toBe('0:55')
  })

  it('logs out at expiry and returns to the same page after logging in', async () => {
    const { auth, router } = await setup()

    vi.advanceTimersByTime(SESSION_MS)
    await flushPromises()

    expect(auth.token).toBeNull()
    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query).toEqual({ redirect: '/requests', expired: '1' })
  })

  it('"Log in again" ends the session without the expired message', async () => {
    const { session, auth, router } = await setup()

    session.logInAgain()
    await flushPromises()

    expect(auth.token).toBeNull()
    expect(router.currentRoute.value.query).toEqual({ redirect: '/requests' })
  })
})
