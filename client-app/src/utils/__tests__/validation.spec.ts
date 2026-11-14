import { describe, expect, it } from 'vitest'
import {
  detectCardBrand,
  isStrongPassword,
  isValidCardNumber,
  isValidExpiry,
  isValidIban,
  validatePaymentMethod,
} from '../validation'

describe('detectCardBrand', () => {
  it.each([
    ['4111111111111111', 'VISA'],
    ['5555555555554444', 'MASTERCARD'],
    ['2221000000000009', 'MASTERCARD'],
    ['378282246310005', 'AMEX'],
    ['6011111111111117', 'OTHER'],
  ])('%s is %s', (number, brand) => {
    expect(detectCardBrand(number)).toBe(brand)
  })
})

describe('isStrongPassword', () => {
  it.each(['Secret-Pass-123', 'Abcdefghij1!'])('accepts %s', (password) => {
    expect(isStrongPassword(password)).toBe(true)
  })

  it.each([
    'Short-1a', // too short
    'alllowercase-123', // no uppercase
    'ALLUPPERCASE-123', // no lowercase
    'NoDigitsHere-!!', // no digit
    'NoSpecial12345', // no special character
  ])('rejects %s', (password) => {
    expect(isStrongPassword(password)).toBe(false)
  })
})

describe('isValidCardNumber', () => {
  it.each(['4111111111111111', '5555 5555 5555 4444', '3782-822463-10005'])(
    'accepts %s',
    (number) => {
      expect(isValidCardNumber(number)).toBe(true)
    },
  )

  it.each(['4111111111111112', '4111', '4111abcd11111111', ''])('rejects %s', (number) => {
    expect(isValidCardNumber(number)).toBe(false)
  })
})

describe('isValidIban', () => {
  it.each(['LV80BANK0000435195001', 'lv80 bank 0000 4351 9500 1', 'DE89370400440532013000'])(
    'accepts %s',
    (iban) => {
      expect(isValidIban(iban)).toBe(true)
    },
  )

  it.each(['LV81BANK0000435195001', 'LV80', ''])('rejects %s', (iban) => {
    expect(isValidIban(iban)).toBe(false)
  })
})

describe('isValidExpiry', () => {
  const now = new Date(2026, 10, 12) // 12 Nov 2026

  it('is valid through the expiry month', () => {
    expect(isValidExpiry('11/26', now)).toBe(true)
    expect(isValidExpiry('10/26', now)).toBe(false)
    expect(isValidExpiry('01/27', now)).toBe(true)
  })

  it('requires MM/YY', () => {
    expect(isValidExpiry('13/27', now)).toBe(false)
    expect(isValidExpiry('1/27', now)).toBe(false)
    expect(isValidExpiry('01/2027', now)).toBe(false)
  })
})

describe('validatePaymentMethod', () => {
  it('reports each invalid card field', () => {
    const errors = validatePaymentMethod({
      type: 'CARD',
      cardNumber: '1234',
      cardHolder: ' ',
      cardExpiry: 'x',
    })
    expect(Object.keys(errors).sort()).toEqual(['cardExpiry', 'cardHolder', 'cardNumber'])
  })

  it('only checks the IBAN for a bank account', () => {
    expect(validatePaymentMethod({ type: 'BANK_ACCOUNT', iban: 'LV80BANK0000435195001' })).toEqual(
      {},
    )
  })
})
