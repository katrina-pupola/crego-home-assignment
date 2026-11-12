package com.crego.backend.paymentmethod;

public enum CardBrand {
    VISA,
    MASTERCARD,
    AMEX,
    OTHER;

    /** Detects the brand from the card number prefix (expects a normalized, valid number). */
    public static CardBrand detect(String number) {
        int firstTwo = Integer.parseInt(number.substring(0, 2));
        int firstFour = Integer.parseInt(number.substring(0, 4));
        if (number.startsWith("4")) {
            return VISA;
        }
        if (firstTwo == 34 || firstTwo == 37) {
            return AMEX;
        }
        if ((firstTwo >= 51 && firstTwo <= 55) || (firstFour >= 2221 && firstFour <= 2720)) {
            return MASTERCARD;
        }
        return OTHER;
    }
}
