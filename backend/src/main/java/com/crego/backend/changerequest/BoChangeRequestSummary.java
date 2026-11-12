package com.crego.backend.changerequest;

import com.crego.backend.paymentmethod.PaymentMethodType;

import java.time.Instant;

/** One row in the BO review queue. */
public record BoChangeRequestSummary(
        Long id,
        ChangeRequestStatus status,
        Long clientId,
        String clientName,
        String clientEmail,
        PaymentMethodType proposedType,
        Instant createdAt,
        Instant reviewedAt
) {

    static BoChangeRequestSummary from(ChangeRequest request) {
        return new BoChangeRequestSummary(request.getId(), request.getStatus(), request.getClient().getId(),
                request.getClient().getFullName(), request.getClient().getEmail(), request.getNewType(),
                request.getCreatedAt(), request.getReviewedAt());
    }
}
