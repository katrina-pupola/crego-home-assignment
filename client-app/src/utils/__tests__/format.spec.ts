import { describe, expect, it } from 'vitest'
import { groupIban } from '../format'

describe('groupIban', () => {
  it.each([
    ['LV80BANK0000435195001', 'LV80 BANK 0000 4351 9500 1'],
    ['DE89370400440532013000', 'DE89 3704 0044 0532 0130 00'],
    ['NL91ABNA0417164300', 'NL91 ABNA 0417 1643 00'],
  ])('%s -> %s', (iban, expected) => {
    expect(groupIban(iban)).toBe(expected)
  })
})
