package com.crego.backend;

import com.crego.backend.changerequest.ChangeRequest;
import com.crego.backend.changerequest.ChangeRequestRepository;
import com.crego.backend.changerequest.ChangeRequestStatus;
import com.crego.backend.client.Client;
import com.crego.backend.client.ClientRepository;
import com.crego.backend.paymentmethod.MethodData;
import com.crego.backend.paymentmethod.PaymentMethod;
import com.crego.backend.paymentmethod.PaymentMethodRepository;
import com.crego.backend.paymentmethod.PaymentMethodStatus;
import com.crego.backend.paymentmethod.PaymentMethodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the rules Postgres enforces on its own, even if service code were bypassed
 * (partial unique indexes and CHECK constraints from the migrations).
 */
@IntegrationTest
class DatabaseConstraintsIntegrationTest {

    private static final MethodData IBAN =
            new MethodData(PaymentMethodType.BANK_ACCOUNT, null, null, null, null, "LV80BANK0000435195001");

    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private PaymentMethodRepository paymentMethodRepository;
    @Autowired
    private ChangeRequestRepository changeRequestRepository;

    private Client client;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setEmail("db-" + UUID.randomUUID() + "@crego.test");
        client.setFullName("Db Test");
        client.setPasswordHash("not-used");
        clientRepository.save(client);
    }

    @Test
    void clientCannotHaveTwoActiveMethods() {
        paymentMethodRepository.saveAndFlush(method(PaymentMethodStatus.ACTIVE, IBAN));

        assertThatThrownBy(() -> paymentMethodRepository.saveAndFlush(method(PaymentMethodStatus.ACTIVE, IBAN)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void clientCannotHaveTwoPendingRequests() {
        changeRequestRepository.saveAndFlush(pendingRequest());

        assertThatThrownBy(() -> changeRequestRepository.saveAndFlush(pendingRequest()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cardRowMustHaveCardFields() {
        MethodData cardWithoutDetails = new MethodData(PaymentMethodType.CARD, null, null, null, null, null);

        assertThatThrownBy(() -> paymentMethodRepository.saveAndFlush(method(PaymentMethodStatus.ACTIVE, cardWithoutDetails)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private PaymentMethod method(PaymentMethodStatus status, MethodData details) {
        PaymentMethod method = new PaymentMethod();
        method.setClient(client);
        method.setStatus(status);
        method.setDetails(details);
        return method;
    }

    private ChangeRequest pendingRequest() {
        ChangeRequest request = new ChangeRequest();
        request.setClient(client);
        request.setStatus(ChangeRequestStatus.PENDING);
        request.setProposed(IBAN);
        return request;
    }
}
