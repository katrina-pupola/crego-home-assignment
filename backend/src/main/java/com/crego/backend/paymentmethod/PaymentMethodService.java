package com.crego.backend.paymentmethod;

import com.crego.backend.client.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public PaymentMethodResponse getActive(long clientId) {
        return paymentMethodRepository.findByClientIdAndStatus(clientId, PaymentMethodStatus.ACTIVE)
                .map(PaymentMethodResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active payment method"));
    }

    // Only the first method is added directly; later changes go through a change request
    @Transactional
    public PaymentMethodResponse addFirst(long clientId, PaymentMethodRequest request) {
        if (paymentMethodRepository.existsByClientIdAndStatus(clientId, PaymentMethodStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A payment method already exists; submit a change request instead");
        }

        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setClient(clientRepository.getReferenceById(clientId));
        paymentMethod.setStatus(PaymentMethodStatus.ACTIVE);
        paymentMethod.setDetails(MethodData.from(request));
        return PaymentMethodResponse.from(paymentMethodRepository.save(paymentMethod));
    }
}
