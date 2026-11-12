import type { CardBrand, ChangeRequestStatus, MethodView } from '@/api/types'

const dateFormat = new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium', timeStyle: 'short' })

export function formatDate(iso: string | null): string {
  return iso ? dateFormat.format(new Date(iso)) : '—'
}

const brandNames: Record<CardBrand, string> = {
  VISA: 'Visa',
  MASTERCARD: 'Mastercard',
  AMEX: 'American Express',
  OTHER: 'Card',
}

/** e.g. "Visa **** 1111" or "Bank account LV** **** **** 5001". */
export function describeMethod(method: MethodView): string {
  if (method.type === 'CARD') {
    return `${brandNames[method.cardBrand ?? 'OTHER']} ${method.maskedNumber}`
  }
  return `Bank account ${method.maskedNumber}`
}

export const statusLabels: Record<ChangeRequestStatus, string> = {
  PENDING: 'Pending review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  CANCELLED: 'Cancelled',
}
