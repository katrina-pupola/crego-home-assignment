package com.crego.backend.paymentmethod;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMethodRequestValidatorTest {

    private static final DateTimeFormatter MM_YY = DateTimeFormatter.ofPattern("MM/yy");

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

    @Test
    void validCardHasNoErrors() {
        assertThat(invalidFields(card("4111111111111111", "Anna Ozola", "12/40"))).isEmpty();
    }

    @Test
    void invalidCardReportsEachFieldSeparately() {
        assertThat(invalidFields(card("4111111111111112", " ", "01/20")))
                .containsExactlyInAnyOrder("cardNumber", "cardHolder", "cardExpiry");
    }

    @Test
    void cardIsValidThroughItsExpiryMonth() {
        String thisMonth = YearMonth.now().format(MM_YY);
        String lastMonth = YearMonth.now().minusMonths(1).format(MM_YY);

        assertThat(invalidFields(card("4111111111111111", "Anna Ozola", thisMonth))).isEmpty();
        assertThat(invalidFields(card("4111111111111111", "Anna Ozola", lastMonth))).containsExactly("cardExpiry");
    }

    @Test
    void expiryMustBeMmYy() {
        assertThat(invalidFields(card("4111111111111111", "Anna Ozola", "13/40"))).containsExactly("cardExpiry");
        assertThat(invalidFields(card("4111111111111111", "Anna Ozola", "12/2040"))).containsExactly("cardExpiry");
    }

    @Test
    void validBankAccountHasNoErrors() {
        assertThat(invalidFields(bankAccount("LV80BANK0000435195001"))).isEmpty();
    }

    @Test
    void invalidIbanIsReported() {
        assertThat(invalidFields(bankAccount("LV81BANK0000435195001"))).containsExactly("iban");
    }

    @Test
    void bankAccountIgnoresCardFields() {
        var request = new PaymentMethodRequest(PaymentMethodType.BANK_ACCOUNT, "garbage", null, "xx", "LV80BANK0000435195001");
        assertThat(invalidFields(request)).isEmpty();
    }

    @Test
    void typeIsRequired() {
        assertThat(invalidFields(new PaymentMethodRequest(null, null, null, null, null))).containsExactly("type");
    }

    @Test
    void toStringDoesNotExposeCardNumber() {
        assertThat(card("4111111111111111", "Anna Ozola", "12/40").toString()).doesNotContain("4111");
    }

    private static Set<String> invalidFields(PaymentMethodRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private static PaymentMethodRequest card(String number, String holder, String expiry) {
        return new PaymentMethodRequest(PaymentMethodType.CARD, number, holder, expiry, null);
    }

    private static PaymentMethodRequest bankAccount(String iban) {
        return new PaymentMethodRequest(PaymentMethodType.BANK_ACCOUNT, null, null, null, iban);
    }
}
