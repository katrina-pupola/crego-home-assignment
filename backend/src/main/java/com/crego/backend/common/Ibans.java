package com.crego.backend.common;

import java.util.Locale;

public final class Ibans {

    private Ibans() {
    }

    /** Removes spaces and uppercases, e.g. "lv80 bank 0000..." -> "LV80BANK0000...". */
    public static String normalize(String raw) {
        return raw == null ? "" : raw.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
    }

    public static boolean isValid(String raw) {
        String iban = normalize(raw);
        return iban.matches("[A-Z]{2}\\d{2}[A-Z0-9]{11,30}") && passesMod97(iban);
    }

    /** "LV80BANK0000435195001" -> "LV** **** **** 5001". */
    public static String mask(String iban) {
        return iban.substring(0, 2) + "** **** **** " + iban.substring(iban.length() - 4);
    }

    // ISO 13616: move the first 4 chars to the end, turn letters into numbers (A=10 ... Z=35);
    // the result mod 97 must be 1. Computed digit by digit to avoid huge numbers.
    static boolean passesMod97(String iban) {
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        int remainder = 0;
        for (char ch : rearranged.toCharArray()) {
            int value = Character.isDigit(ch) ? ch - '0' : ch - 'A' + 10;
            remainder = (value < 10 ? remainder * 10 : remainder * 100) + value;
            remainder %= 97;
        }
        return remainder == 1;
    }
}
