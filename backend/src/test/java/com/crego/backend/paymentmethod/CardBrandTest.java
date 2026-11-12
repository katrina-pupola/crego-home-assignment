package com.crego.backend.paymentmethod;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CardBrandTest {

    @ParameterizedTest
    @CsvSource({
            "4111111111111111, VISA",
            "5555555555554444, MASTERCARD",
            "2221000000000009, MASTERCARD",  // newer 2-series range
            "378282246310005,  AMEX",
            "6011111111111117, OTHER"        // Discover
    })
    void detectsBrandFromPrefix(String number, CardBrand expected) {
        assertThat(CardBrand.detect(number)).isEqualTo(expected);
    }
}
