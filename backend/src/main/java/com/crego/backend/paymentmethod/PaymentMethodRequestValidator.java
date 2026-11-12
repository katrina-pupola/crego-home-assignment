package com.crego.backend.paymentmethod;

import com.crego.backend.common.CardNumbers;
import com.crego.backend.common.Ibans;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.YearMonth;

public class PaymentMethodRequestValidator implements ConstraintValidator<ValidPaymentMethod, PaymentMethodRequest> {

    @Override
    public boolean isValid(PaymentMethodRequest request, ConstraintValidatorContext context) {
        if (request == null || request.type() == null) {
            return true; // @NotNull on type reports this case
        }
        context.disableDefaultConstraintViolation();

        boolean valid = true;
        if (request.type() == PaymentMethodType.CARD) {
            if (!CardNumbers.isValid(request.cardNumber())) {
                valid = reject(context, "cardNumber", "Invalid card number");
            }
            if (request.cardHolder() == null || request.cardHolder().isBlank()) {
                valid = reject(context, "cardHolder", "Card holder is required");
            }
            if (!isValidExpiry(request.cardExpiry())) {
                valid = reject(context, "cardExpiry", "Expiry must be a current or future date in MM/YY format");
            }
        } else if (!Ibans.isValid(request.iban())) {
            valid = reject(context, "iban", "Invalid IBAN");
        }
        return valid;
    }

    static boolean isValidExpiry(String expiry) {
        if (expiry == null || !expiry.matches("(0[1-9]|1[0-2])/\\d{2}")) {
            return false;
        }
        int month = Integer.parseInt(expiry.substring(0, 2));
        int year = 2000 + Integer.parseInt(expiry.substring(3));
        return !YearMonth.of(year, month).isBefore(YearMonth.now()); // a card is valid through its expiry month
    }

    // Reports the error on a specific field, so the frontend can show it next to that input
    private static boolean reject(ConstraintValidatorContext context, String field, String message) {
        context.buildConstraintViolationWithTemplate(message).addPropertyNode(field).addConstraintViolation();
        return false;
    }
}
