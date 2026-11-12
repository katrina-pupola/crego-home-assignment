package com.crego.backend.changerequest;

import com.crego.backend.paymentmethod.MethodData;
import com.crego.backend.paymentmethod.PaymentMethod;
import com.crego.backend.paymentmethod.PaymentMethodRepository;
import com.crego.backend.paymentmethod.PaymentMethodRequest;
import com.crego.backend.paymentmethod.PaymentMethodStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChangeRequestService {

    private final ChangeRequestRepository changeRequestRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    @Transactional
    public ChangeRequestResponse create(long clientId, PaymentMethodRequest request) {
        PaymentMethod current = paymentMethodRepository.findByClientIdAndStatus(clientId, PaymentMethodStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Add a payment method before requesting a change"));
        // Also enforced by the one_pending_per_client index for simultaneous requests
        if (changeRequestRepository.existsByClientIdAndStatus(clientId, ChangeRequestStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A change request is already pending");
        }

        ChangeRequest changeRequest = new ChangeRequest();
        changeRequest.setClient(current.getClient());
        changeRequest.setCurrentMethod(current);
        changeRequest.setProposed(MethodData.from(request));
        changeRequest.setStatus(ChangeRequestStatus.PENDING);
        return ChangeRequestResponse.from(changeRequestRepository.save(changeRequest));
    }

    @Transactional(readOnly = true)
    public List<ChangeRequestResponse> listOwn(long clientId) {
        return changeRequestRepository.findByClientIdOrderByCreatedAtDesc(clientId).stream()
                .map(ChangeRequestResponse::from)
                .toList();
    }

    @Transactional
    public ChangeRequestResponse cancel(long clientId, long requestId) {
        ChangeRequest changeRequest = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Change request not found"));
        if (!changeRequest.getClient().getId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your change request");
        }
        if (changeRequest.getStatus() != ChangeRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending requests can be cancelled");
        }

        changeRequest.setStatus(ChangeRequestStatus.CANCELLED); // saved on commit; @Version guards against a parallel BO decision
        return ChangeRequestResponse.from(changeRequest);
    }
}
