package com.crego.backend.auth;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Secret-Pass-123", "Abcdefghij1!", "Žirafe#2026Riga"})
    void acceptsStrongPasswords(String password) {
        assertThat(validator.validate(request(password))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Short-1a",          // too short
            "alllowercase-123",  // no uppercase
            "ALLUPPERCASE-123",  // no lowercase
            "NoDigitsHere-!!",   // no digit
            "NoSpecial12345",    // no special character
            "Aa1!āāāāāāāāāāāāāāāāāāāāāāāāāāāāāāāāāāā" // 39 characters but 74 bytes (ā takes two)
    })
    void rejectsWeakPasswords(String password) {
        assertThat(validator.validate(request(password)))
                .extracting(v -> v.getPropertyPath().toString())
                .containsOnly("password");
    }

    private static RegisterRequest request(String password) {
        return new RegisterRequest("Anna Ozola", "anna@example.test", password);
    }
}
