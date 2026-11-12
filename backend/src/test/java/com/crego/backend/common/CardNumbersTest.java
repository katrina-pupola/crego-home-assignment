package com.crego.backend.common;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CardNumbersTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "4111111111111111",      // Visa test number
            "5555555555554444",      // Mastercard test number
            "378282246310005",       // Amex test number (15 digits)
            "4111 1111 1111 1111",   // spaces allowed
            "4111-1111-1111-1111"    // dashes allowed
    })
    void acceptsValidCardNumbers(String number) {
        assertThat(CardNumbers.isValid(number)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "4111111111111112",      // last digit wrong: fails Luhn
            "41111111111",           // too short
            "41111111111111111111",  // too long
            "4111abcd11111111"       // letters
    })
    void rejectsInvalidCardNumbers(String number) {
        assertThat(CardNumbers.isValid(number)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"4111 1111 1111 1111", "4111-1111-1111-1111", " 4111111111111111 "})
    void normalizeRemovesSpacesAndDashes(String number) {
        assertThat(CardNumbers.normalize(number)).isEqualTo("4111111111111111");
    }
}
