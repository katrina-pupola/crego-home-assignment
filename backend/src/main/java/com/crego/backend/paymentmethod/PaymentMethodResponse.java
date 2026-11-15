package com.crego.backend.paymentmethod;

import java.time.Instant;

public record PaymentMethodResponse(Long id, PaymentMethodStatus status, MethodView method, Instant createdAt) {

    static PaymentMethodResponse from(PaymentMethod paymentMethod) {
        return new PaymentMethodResponse(paymentMethod.getId(), paymentMethod.getStatus(),
                MethodView.from(paymentMethod.getDetails()), paymentMethod.getCreatedAt());
    }
}
