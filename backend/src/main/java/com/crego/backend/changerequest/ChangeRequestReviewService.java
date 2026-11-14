package com.crego.backend.changerequest;

import com.crego.backend.auth.BoUserRepository;
import com.crego.backend.common.CardExpiries;
import com.crego.backend.common.PageResponse;
import com.crego.backend.notification.ChangeRequestDecided;
import com.crego.backend.paymentmethod.MethodData;
import com.crego.backend.paymentmethod.PaymentMethod;
import com.crego.backend.paymentmethod.PaymentMethodRepository;
import com.crego.backend.paymentmethod.PaymentMethodStatus;
import com.crego.backend.paymentmethod.PaymentMethodType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.YearMonth;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ChangeRequestReviewService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ChangeRequestRepository changeRequestRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final BoUserRepository boUserRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** Newest first; all statuses when status is null. */
    @Transactional(readOnly = true)
    public PageResponse<BoChangeRequestSummary> list(ChangeRequestStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ChangeRequest> result = status == null
                ? changeRequestRepository.findAll(pageable)
                : changeRequestRepository.findByStatus(status, pageable);
        return PageResponse.of(result.map(BoChangeRequestSummary::from));
    }

    @Transactional(readOnly = true)
    public BoChangeRequestDetail get(long id) {
        return BoChangeRequestDetail.from(findOrThrow(id));
    }

    // One transaction: if any step fails, nothing is saved
    @Transactional
    public BoChangeRequestDetail approve(long id, long reviewerId, String comment) {
        ChangeRequest changeRequest = findOrThrow(id);
        requirePending(changeRequest);
        // The card may have expired while the request waited for review
        MethodData proposed = changeRequest.getProposed();
        if (proposed.type() == PaymentMethodType.CARD
                && !CardExpiries.isValid(proposed.cardExpiry(), YearMonth.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The proposed card has expired; reject the request instead");
        }
        long clientId = changeRequest.getClient().getId();

        paymentMethodRepository.findByClientIdAndStatus(clientId, PaymentMethodStatus.ACTIVE)
                .ifPresent(old -> old.setStatus(PaymentMethodStatus.REPLACED));
        // Write the REPLACED status before inserting the new ACTIVE row; otherwise the
        // one-active-method index would briefly see two active methods and reject the insert
        paymentMethodRepository.flush();

        PaymentMethod newMethod = new PaymentMethod();
        newMethod.setClient(changeRequest.getClient());
        newMethod.setStatus(PaymentMethodStatus.ACTIVE);
        newMethod.setDetails(proposed);
        paymentMethodRepository.save(newMethod);

        decide(changeRequest, ChangeRequestStatus.APPROVED, reviewerId, comment);
        return BoChangeRequestDetail.from(changeRequest);
    }

    // Only the request changes; the client's active method stays as it was
    @Transactional
    public BoChangeRequestDetail reject(long id, long reviewerId, String reason) {
        ChangeRequest changeRequest = findOrThrow(id);
        requirePending(changeRequest);
        decide(changeRequest, ChangeRequestStatus.REJECTED, reviewerId, reason);
        return BoChangeRequestDetail.from(changeRequest);
    }

    // Saved on commit; @Version makes a parallel decision on the same request fail with 409
    private void decide(ChangeRequest changeRequest, ChangeRequestStatus status, long reviewerId, String comment) {
        changeRequest.setStatus(status);
        changeRequest.setReviewer(boUserRepository.getReferenceById(reviewerId));
        changeRequest.setReviewComment(comment == null || comment.isBlank() ? null : comment.trim());
        changeRequest.setReviewedAt(Instant.now());

        // Emailed only if this transaction commits, i.e. the decision was really saved
        eventPublisher.publishEvent(new ChangeRequestDecided(changeRequest.getClient().getEmail(),
                changeRequest.getClient().getFullName(), status == ChangeRequestStatus.APPROVED,
                changeRequest.getReviewComment()));
    }

    private ChangeRequest findOrThrow(long id) {
        return changeRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Change request not found"));
    }

    private static void requirePending(ChangeRequest changeRequest) {
        if (changeRequest.getStatus() != ChangeRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This request is already " + changeRequest.getStatus().name().toLowerCase(Locale.ROOT));
        }
    }
}
