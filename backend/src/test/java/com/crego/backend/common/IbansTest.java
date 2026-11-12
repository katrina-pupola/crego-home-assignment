package com.crego.backend.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class IbansTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "LV80BANK0000435195001",       // Latvia
            "DE89370400440532013000",      // Germany
            "GB82WEST12345698765432",      // United Kingdom
            "lv80 bank 0000 4351 9500 1"   // lowercase with spaces
    })
    void acceptsValidIbans(String iban) {
        assertThat(Ibans.isValid(iban)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "LV81BANK0000435195001",   // wrong check digits
            "LV80BANK0000435195002",   // one digit changed
            "LV80",                    // too short
            "1234567890123456"         // no country code
    })
    void rejectsInvalidIbans(String iban) {
        assertThat(Ibans.isValid(iban)).isFalse();
    }

    @Test
    void normalizeRemovesSpacesAndUppercases() {
        assertThat(Ibans.normalize("lv80 bank 0000 4351 9500 1")).isEqualTo("LV80BANK0000435195001");
    }

    @Test
    void maskShowsOnlyCountryAndLastFourCharacters() {
        assertThat(Ibans.mask("LV80BANK0000435195001")).isEqualTo("LV** **** **** 5001");
    }
}
