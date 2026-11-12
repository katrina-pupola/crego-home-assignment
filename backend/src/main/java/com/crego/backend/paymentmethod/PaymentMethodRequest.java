package com.crego.backend.paymentmethod;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for adding a first method and for change requests.
 * The full card number is only used for validation and is never stored.
 */
@ValidPaymentMethod
public record PaymentMethodRequest(
        @NotNull(message = "Payment method type is required") PaymentMethodType type,
        String cardNumber,
        @Size(max = 255) String cardHolder,
        String cardExpiry,
        String iban
) {

    // Keeps the card number out of logs and error messages
    @Override
    public String toString() {
        return "PaymentMethodRequest[type=" + type + "]";
    }
}
