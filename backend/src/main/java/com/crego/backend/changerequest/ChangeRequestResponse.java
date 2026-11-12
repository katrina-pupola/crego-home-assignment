package com.crego.backend.changerequest;

import com.crego.backend.paymentmethod.MethodView;

import java.time.Instant;

public record ChangeRequestResponse(
        Long id,
        ChangeRequestStatus status,
        MethodView proposedMethod,
        String reviewComment,
        Instant createdAt,
        Instant reviewedAt
) {

    public static ChangeRequestResponse from(ChangeRequest request) {
        return new ChangeRequestResponse(request.getId(), request.getStatus(), MethodView.of(request.getProposed()),
                request.getReviewComment(), request.getCreatedAt(), request.getReviewedAt());
    }
}
