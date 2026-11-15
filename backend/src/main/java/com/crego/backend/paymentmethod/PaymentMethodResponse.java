package com.crego.backend.paymentmethod;

import java.time.Instant;

/**
 * The client's own payment method. {@code iban} is the full IBAN for bank accounts, so the owner can
 * check and copy it; it is only returned from the client's own endpoints. Card numbers are never
 * stored in full, so cards only have the masked number in {@code method}.
 */
public record PaymentMethodResponse(Long id, PaymentMethodStatus status, MethodView method, String iban,
                                    Instant createdAt) {

    static PaymentMethodResponse from(PaymentMethod paymentMethod) {
        return new PaymentMethodResponse(paymentMethod.getId(), paymentMethod.getStatus(),
                MethodView.from(paymentMethod.getDetails()), paymentMethod.getIban(), paymentMethod.getCreatedAt());
    }
}
