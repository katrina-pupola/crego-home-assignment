// Mirrors the backend DTOs (Java records)

export type PaymentMethodType = 'CARD' | 'BANK_ACCOUNT'
export type CardBrand = 'VISA' | 'MASTERCARD' | 'AMEX' | 'OTHER'
export type ChangeRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'

export interface TokenResponse {
  token: string
  expiresAt: string
}

export interface RegistrationResponse {
  email: string
}

export interface ClientProfile {
  id: number
  email: string
  fullName: string
  createdAt: string
}

export interface MethodView {
  type: PaymentMethodType
  cardBrand: CardBrand | null
  cardHolder: string | null
  cardExpiry: string | null
  maskedNumber: string
}

export interface PaymentMethodResponse {
  id: number
  status: 'ACTIVE' | 'REPLACED'
  method: MethodView
  createdAt: string
}

export interface ChangeRequestResponse {
  id: number
  status: ChangeRequestStatus
  proposedMethod: MethodView
  reviewComment: string | null
  createdAt: string
  reviewedAt: string | null
}

export interface PaymentMethodRequest {
  type: PaymentMethodType
  cardNumber?: string
  cardHolder?: string
  cardExpiry?: string
  iban?: string
}

export interface ProblemDetail {
  status: number
  title?: string
  detail?: string
  code?: string
  errors?: Record<string, string>
}
