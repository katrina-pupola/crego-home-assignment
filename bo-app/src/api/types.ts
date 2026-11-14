// Mirrors the backend DTOs (Java records)

export type PaymentMethodType = 'CARD' | 'BANK_ACCOUNT'
export type CardBrand = 'VISA' | 'MASTERCARD' | 'AMEX' | 'OTHER'
export type ChangeRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'

export interface TokenResponse {
  token: string
  expiresAt: string
}

export interface BoUserProfile {
  id: number
  email: string
  fullName: string
}

export interface MethodView {
  type: PaymentMethodType
  cardBrand: CardBrand | null
  cardHolder: string | null
  cardExpiry: string | null
  maskedNumber: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/** One row in the review queue. */
export interface BoChangeRequestSummary {
  id: number
  status: ChangeRequestStatus
  clientId: number
  clientName: string
  clientEmail: string
  proposedType: PaymentMethodType
  createdAt: string
  reviewedAt: string | null
}

export interface BoChangeRequestDetail {
  id: number
  status: ChangeRequestStatus
  client: { id: number; fullName: string; email: string }
  currentMethod: MethodView | null
  proposedMethod: MethodView
  reviewerName: string | null
  reviewComment: string | null
  createdAt: string
  reviewedAt: string | null
}

export interface ProblemDetail {
  status: number
  title?: string
  detail?: string
  errors?: Record<string, string>
}
