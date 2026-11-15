package com.crego.backend.changerequest;

import com.crego.backend.ApiTestSupport.RegisteredClient;
import com.crego.backend.IntegrationTest;
import com.crego.backend.RecordingMailSender;
import com.crego.backend.auth.BoUserRepository;
import com.crego.backend.client.Client;
import com.crego.backend.client.ClientRepository;
import com.crego.backend.paymentmethod.CardBrand;
import com.crego.backend.paymentmethod.MethodData;
import com.crego.backend.paymentmethod.PaymentMethod;
import com.crego.backend.paymentmethod.PaymentMethodRepository;
import com.crego.backend.paymentmethod.PaymentMethodStatus;
import com.crego.backend.paymentmethod.PaymentMethodType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.crego.backend.ApiTestSupport.IBAN_JSON;
import static com.crego.backend.ApiTestSupport.addCard;
import static com.crego.backend.ApiTestSupport.bearer;
import static com.crego.backend.ApiTestSupport.loginBo;
import static com.crego.backend.ApiTestSupport.loginDemoClient;
import static com.crego.backend.ApiTestSupport.registerNewClient;
import static com.crego.backend.ApiTestSupport.requestIbanChange;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Business rules around change requests: simultaneous decisions, cancelling, filtering, one direct add. */
@IntegrationTest
class ChangeRequestRulesIntegrationTest {

    private static final MethodData VISA = new MethodData(PaymentMethodType.CARD, "1111", CardBrand.VISA,
            "Db Test", "12/40", null);
    private static final MethodData IBAN = new MethodData(PaymentMethodType.BANK_ACCOUNT, null, null, null, null,
            "LV80BANK0000435195001");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private RecordingMailSender mail;
    @Autowired
    private ChangeRequestReviewService reviewService;
    @Autowired
    private ChangeRequestRepository changeRequestRepository;
    @Autowired
    private PaymentMethodRepository paymentMethodRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private BoUserRepository boUserRepository;

    @Test
    void staleWriteAfterADecisionIsRejectedByVersionCheck() {
        Client client = newClient();
        ChangeRequest request = pendingRequest(client);

        // Reviewer B loaded the request before reviewer A approved it
        ChangeRequest seenByB = changeRequestRepository.findById(request.getId()).orElseThrow();
        long reviewerA = boUserRepository.findByEmail("admin@crego.test").orElseThrow().getId();
        reviewService.approve(request.getId(), reviewerA, null);

        seenByB.setStatus(ChangeRequestStatus.REJECTED);
        assertThatThrownBy(() -> changeRequestRepository.save(seenByB))
                .isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(changeRequestRepository.findById(request.getId()).orElseThrow().getStatus())
                .isEqualTo(ChangeRequestStatus.APPROVED);
        assertThat(paymentMethodRepository.findByClientIdAndStatus(client.getId(), PaymentMethodStatus.ACTIVE))
                .hasValueSatisfying(m -> assertThat(m.getIban()).isEqualTo("LV80BANK0000435195001"));
        assertThat(mail.sentTo(client.getEmail())).hasSize(1);
    }

    @Test
    void clientCanCancelOwnPendingRequestOnlyOnce() throws Exception {
        RegisteredClient client = registerNewClient(mvc, mail);
        addCard(mvc, client.token());
        Integer requestId = requestIbanChange(mvc, client.token());

        mvc.perform(post("/api/client/change-requests/{id}/cancel", requestId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginDemoClient(mvc))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/client/change-requests/{id}/cancel", requestId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(client.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(post("/api/client/change-requests/{id}/cancel", requestId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(client.token())))
                .andExpect(status().isConflict());
    }

    @Test
    void boQueueFiltersByStatus() throws Exception {
        RegisteredClient client = registerNewClient(mvc, mail);
        addCard(mvc, client.token());
        Integer requestId = requestIbanChange(mvc, client.token());
        String boToken = loginBo(mvc);

        mvc.perform(get("/api/bo/change-requests").param("status", "PENDING").param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, bearer(boToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(requestId)))
                .andExpect(jsonPath("$.content[*].status", not(hasItem("APPROVED"))));
        mvc.perform(get("/api/bo/change-requests").param("status", "APPROVED").param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, bearer(boToken)))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(requestId))));
    }

    @Test
    void secondDirectAddReturnsConflict() throws Exception {
        RegisteredClient client = registerNewClient(mvc, mail);
        addCard(mvc, client.token());
        mvc.perform(post("/api/client/payment-method").header(HttpHeaders.AUTHORIZATION, bearer(client.token()))
                        .contentType(MediaType.APPLICATION_JSON).content(IBAN_JSON))
                .andExpect(status().isConflict());
    }

    private Client newClient() {
        Client client = new Client();
        client.setEmail("race-" + UUID.randomUUID() + "@crego.test");
        client.setFullName("Db Test");
        client.setPasswordHash("not-used");
        return clientRepository.save(client);
    }

    private ChangeRequest pendingRequest(Client client) {
        PaymentMethod method = new PaymentMethod();
        method.setClient(client);
        method.setStatus(PaymentMethodStatus.ACTIVE);
        method.setDetails(VISA);
        paymentMethodRepository.save(method);

        ChangeRequest request = new ChangeRequest();
        request.setClient(client);
        request.setCurrentMethod(method);
        request.setStatus(ChangeRequestStatus.PENDING);
        request.setProposed(IBAN);
        return changeRequestRepository.save(request);
    }
}
