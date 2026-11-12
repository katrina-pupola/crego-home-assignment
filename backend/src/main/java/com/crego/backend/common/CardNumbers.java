package com.crego.backend.common;

public final class CardNumbers {

    private CardNumbers() {
    }

    /** Removes spaces and dashes, e.g. "4111 1111-1111 1111" -> "4111111111111111". */
    public static String normalize(String raw) {
        return raw == null ? "" : raw.replaceAll("[\\s-]", "");
    }

    public static boolean isValid(String raw) {
        String number = normalize(raw);
        return number.matches("\\d{12,19}") && passesLuhn(number);
    }

    // Luhn: from the right, double every second digit (minus 9 if > 9); the sum must end in 0
    static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';
            if (doubleIt) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }
}
