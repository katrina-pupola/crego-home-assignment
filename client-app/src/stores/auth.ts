import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as api from '@/api/client'
import type { ClientProfile } from '@/api/types'

const TOKEN_KEY = 'crego.client.token'

interface TokenPayload {
  sub: string
  exp: number
}

// Reads the JWT payload (middle part). Only for expiry and user id; the backend verifies the signature.
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
  const profile = ref<ClientProfile | null>(null)

  const payload = computed(() => (token.value ? decode(token.value) : null))
  // Expiry time in milliseconds, read from the token's "exp" claim
  const expiresAt = computed(() => (payload.value ? payload.value.exp * 1000 : null))
  const isLoggedIn = computed(() => expiresAt.value !== null && expiresAt.value > Date.now())
  const userId = computed(() => payload.value?.sub ?? null)

  function setToken(value: string) {
    token.value = value
    localStorage.setItem(TOKEN_KEY, value)
  }

  async function login(email: string, password: string) {
    setToken((await api.login(email, password)).token)
  }

  // Registering doesn't log in yet: the client confirms the email first
  async function register(fullName: string, email: string, password: string) {
    return (await api.register(fullName, email, password)).email
  }

  // Clicking the email link confirms the address and logs the client in
  async function verifyEmail(verificationToken: string) {
    setToken((await api.verifyEmail(verificationToken)).token)
  }

  async function loadProfile() {
    profile.value = await api.getProfile()
  }

  function logout() {
    token.value = null
    profile.value = null
    localStorage.removeItem(TOKEN_KEY)
  }

  return {
    token,
    profile,
    expiresAt,
    isLoggedIn,
    userId,
    login,
    register,
    verifyEmail,
    loadProfile,
    logout,
  }
})
