import axios from 'axios'
import { http } from './http'
import type {
  ChangeRequestResponse,
  ClientProfile,
  PaymentMethodRequest,
  PaymentMethodResponse,
  RegistrationResponse,
  TokenResponse,
} from './types'

/** Creates the account and sends a verification email; no token until the link is clicked. */
export async function register(fullName: string, email: string, password: string) {
  return (
    await http.post<RegistrationResponse>('/auth/client/register', { fullName, email, password })
  ).data
}

/** Exchanges the token from the email link for a login token. */
export async function verifyEmail(token: string) {
  return (await http.post<TokenResponse>('/auth/client/verify', { token })).data
}

export async function resendVerification(email: string) {
  await http.post('/auth/client/resend-verification', { email })
}

export async function login(email: string, password: string) {
  return (await http.post<TokenResponse>('/auth/client/login', { email, password })).data
}

export async function getProfile() {
  return (await http.get<ClientProfile>('/client/me')).data
}

/** The active method, or null if the client has not added one yet (404). */
export async function getActivePaymentMethod() {
  try {
    return (await http.get<PaymentMethodResponse>('/client/payment-method')).data
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      return null
    }
    throw error
  }
}

export async function addFirstPaymentMethod(request: PaymentMethodRequest) {
  return (await http.post<PaymentMethodResponse>('/client/payment-method', request)).data
}

export async function createChangeRequest(request: PaymentMethodRequest) {
  return (await http.post<ChangeRequestResponse>('/client/change-requests', request)).data
}

/** Newest first. */
export async function listChangeRequests() {
  return (await http.get<ChangeRequestResponse[]>('/client/change-requests')).data
}

export async function cancelChangeRequest(id: number) {
  return (await http.post<ChangeRequestResponse>(`/client/change-requests/${id}/cancel`)).data
}
