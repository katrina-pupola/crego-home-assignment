import { describe, expect, it } from 'vitest'
import type { MethodView } from '@/api/types'
import { brandName, countryName, displayNumber, initials } from '../format'

const card: MethodView = {
  type: 'CARD',
  cardBrand: 'VISA',
  cardHolder: 'Anna Ozola',
  cardExpiry: '12/28',
  maskedNumber: '**** 1111',
}

const bankAccount: MethodView = {
  type: 'BANK_ACCOUNT',
  cardBrand: null,
  cardHolder: null,
  cardExpiry: null,
  maskedNumber: 'LV** **** **** 5001',
}

describe('displayNumber', () => {
  it('shows only the last 4 card digits', () => {
    expect(displayNumber(card)).toBe('•••• 1111')
  })

  it('keeps the IBAN mask with bullets', () => {
    expect(displayNumber(bankAccount)).toBe('LV•• •••• •••• 5001')
  })
})

describe('brandName', () => {
  it('names known brands and falls back to "Card"', () => {
    expect(brandName('VISA')).toBe('Visa')
    expect(brandName(null)).toBe('Card')
  })
})

describe('initials', () => {
  it.each([
    ['Crego Admin', 'CA'],
    ['anna', 'A'],
    ['Anna Marija Ozola', 'AM'],
  ])('%s -> %s', (name, expected) => {
    expect(initials(name)).toBe(expected)
  })
})

describe('countryName', () => {
  it('turns a country code into a name', () => {
    expect(countryName('LV')).toBe('Latvia')
  })
})
