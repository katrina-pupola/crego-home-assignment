import type { CardBrand, ChangeRequestStatus, MethodView, PaymentMethodType } from '@/api/types'

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

export const typeLabels: Record<PaymentMethodType, string> = {
  CARD: 'Bank card',
  BANK_ACCOUNT: 'Bank account',
}

/** Number as shown to staff: "•••• 1111" for cards, "LV•• •••• •••• 5001" for IBANs. */
export function displayNumber(method: MethodView): string {
  return method.type === 'CARD'
    ? `•••• ${method.maskedNumber.slice(-4)}`
    : method.maskedNumber.replace(/\*/g, '•')
}

export function brandName(brand: CardBrand | null): string {
  return brandNames[brand ?? 'OTHER']
}

const regionNames = new Intl.DisplayNames(['en'], { type: 'region' })

/** "LV" -> "Latvia"; falls back to the code itself. */
export function countryName(code: string): string {
  try {
    return regionNames.of(code) ?? code
  } catch {
    return code
  }
}

/** "Anna Ozola" -> "AO". */
export function initials(fullName: string): string {
  return fullName
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('')
}

export const statusLabels: Record<ChangeRequestStatus, string> = {
  PENDING: 'Pending review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  CANCELLED: 'Cancelled',
}
