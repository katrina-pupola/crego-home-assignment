package com.crego.backend.changerequest;

import com.crego.backend.client.Client;
import com.crego.backend.paymentmethod.MethodView;

import java.time.Instant;

/** Full request for the BO detail page: current vs proposed method side by side. */
public record BoChangeRequestDetail(
        Long id,
        ChangeRequestStatus status,
        ClientInfo client,
        MethodView currentMethod,
        MethodView proposedMethod,
        String reviewerName,
        String reviewComment,
        Instant createdAt,
        Instant reviewedAt
) {

    public record ClientInfo(Long id, String fullName, String email) {

        static ClientInfo from(Client client) {
            return new ClientInfo(client.getId(), client.getFullName(), client.getEmail());
        }
    }

    static BoChangeRequestDetail from(ChangeRequest request) {
        return new BoChangeRequestDetail(
                request.getId(),
                request.getStatus(),
                ClientInfo.from(request.getClient()),
                request.getCurrentMethod() == null ? null : MethodView.from(request.getCurrentMethod().getDetails()),
                MethodView.from(request.getProposed()),
                request.getReviewer() == null ? null : request.getReviewer().getFullName(),
                request.getReviewComment(),
                request.getCreatedAt(),
                request.getReviewedAt());
    }
}
