// Same rules as the backend, so most mistakes are caught before submitting.
// The backend still validates everything; this is only for faster feedback.

import type { PaymentMethodRequest } from '@/api/types'

export function normalizeCardNumber(raw: string): string {
  return raw.replace(/[\s-]/g, '')
}

export function isValidCardNumber(raw: string): boolean {
  const digits = normalizeCardNumber(raw)
  if (!/^\d{12,19}$/.test(digits)) return false

  // Luhn: from the right, double every second digit (minus 9 if > 9); the sum must end in 0
  let sum = 0
  let doubleIt = false
  for (let i = digits.length - 1; i >= 0; i--) {
    let digit = Number(digits[i])
    if (doubleIt) {
      digit *= 2
      if (digit > 9) digit -= 9
    }
    sum += digit
    doubleIt = !doubleIt
  }
  return sum % 10 === 0
}

export function normalizeIban(raw: string): string {
  return raw.replace(/\s/g, '').toUpperCase()
}

export function isValidIban(raw: string): boolean {
  const iban = normalizeIban(raw)
  if (!/^[A-Z]{2}\d{2}[A-Z0-9]{11,30}$/.test(iban)) return false

  // Mod-97: move the first 4 chars to the end, letters become numbers (A=10 ... Z=35)
  const rearranged = iban.slice(4) + iban.slice(0, 4)
  let remainder = 0
  for (const ch of rearranged) {
    const value = /\d/.test(ch) ? Number(ch) : ch.charCodeAt(0) - 55
    remainder = ((value < 10 ? remainder * 10 : remainder * 100) + value) % 97
  }
  return remainder === 1
}

/** MM/YY, valid through the end of that month. */
export function isValidExpiry(expiry: string, now: Date = new Date()): boolean {
  const match = /^(0[1-9]|1[0-2])\/(\d{2})$/.exec(expiry)
  if (!match) return false
  const month = Number(match[1])
  const year = 2000 + Number(match[2])
  return year * 12 + month >= now.getFullYear() * 12 + (now.getMonth() + 1)
}

/** Same policy as the backend's RegisterRequest; shown as a live checklist on the register page. */
export const passwordRules: { label: string; test: (password: string) => boolean }[] = [
  { label: 'At least 12 characters', test: (p) => p.length >= 12 && p.length <= 72 },
  { label: 'An uppercase letter', test: (p) => /[A-Z]/.test(p) },
  { label: 'A lowercase letter', test: (p) => /[a-z]/.test(p) },
  { label: 'A digit', test: (p) => /\d/.test(p) },
  { label: 'A special character (e.g. ! # - _)', test: (p) => /[^A-Za-z0-9]/.test(p) },
]

export function isStrongPassword(password: string): boolean {
  return passwordRules.every((rule) => rule.test(password))
}

/** Returns errors keyed by field name, matching the backend's field names. */
export function validatePaymentMethod(request: PaymentMethodRequest): Record<string, string> {
  const errors: Record<string, string> = {}
  if (request.type === 'CARD') {
    if (!isValidCardNumber(request.cardNumber ?? '')) errors.cardNumber = 'Invalid card number'
    if (!request.cardHolder?.trim()) errors.cardHolder = 'Card holder is required'
    if (!isValidExpiry(request.cardExpiry ?? '')) errors.cardExpiry = 'Use MM/YY, not in the past'
  } else if (!isValidIban(request.iban ?? '')) {
    errors.iban = 'Invalid IBAN'
  }
  return errors
}
