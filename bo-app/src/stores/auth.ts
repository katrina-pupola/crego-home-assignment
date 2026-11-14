import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as api from '@/api/bo'
import type { BoUserProfile } from '@/api/types'

const TOKEN_KEY = 'bo.token'

interface TokenPayload {
  sub: string
  exp: number
  role: string
}

// Reads the JWT payload (middle part). Only for expiry and role; the backend verifies the signature.
function decode(token: string): TokenPayload | null {
  try {
    const part = token.split('.')[1] ?? ''
    return JSON.parse(atob(part.replace(/-/g, '+').replace(/_/g, '/'))) as TokenPayload
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  // Kept in localStorage so a page refresh keeps the user logged in
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const profile = ref<BoUserProfile | null>(null)

  const payload = computed(() => (token.value ? decode(token.value) : null))
  // Expiry time in milliseconds, read from the token's "exp" claim
  const expiresAt = computed(() => (payload.value ? payload.value.exp * 1000 : null))
  const isLoggedIn = computed(
    () => payload.value?.role === 'BO' && expiresAt.value !== null && expiresAt.value > Date.now(),
  )

  // Checked on every call; a computed would cache the result because Date.now() is not reactive
  function hasValidSession() {
    return payload.value?.role === 'BO' && expiresAt.value !== null && expiresAt.value > Date.now()
  }

  async function login(email: string, password: string) {
    token.value = (await api.login(email, password)).token
    localStorage.setItem(TOKEN_KEY, token.value)
  }

  async function loadProfile() {
    profile.value = await api.getProfile()
  }

  function logout() {
    token.value = null
    profile.value = null
    localStorage.removeItem(TOKEN_KEY)
  }

  return { token, profile, expiresAt, isLoggedIn, hasValidSession, login, loadProfile, logout }
})
