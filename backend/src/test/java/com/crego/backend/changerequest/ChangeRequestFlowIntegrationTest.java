package com.crego.backend.changerequest;

import com.crego.backend.ApiTestSupport.RegisteredClient;
import com.crego.backend.IntegrationTest;
import com.crego.backend.RecordingMailSender;
import com.crego.backend.RecordingMailSender.SentEmail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.crego.backend.ApiTestSupport.CARD_JSON;
import static com.crego.backend.ApiTestSupport.IBAN_JSON;
import static com.crego.backend.ApiTestSupport.addCard;
import static com.crego.backend.ApiTestSupport.bearer;
import static com.crego.backend.ApiTestSupport.json;
import static com.crego.backend.ApiTestSupport.loginBo;
import static com.crego.backend.ApiTestSupport.registerNewClient;
import static com.crego.backend.ApiTestSupport.requestIbanChange;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The whole task end to end: register, add a method, request a change, BO approves, client sees it. */
@IntegrationTest
class ChangeRequestFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RecordingMailSender mail;

    @Test
    void approvedChangeBecomesTheClientsActiveMethod() throws Exception {
        RegisteredClient client = registerNewClient(mvc, mail);
        String clientToken = client.token();

        mvc.perform(post("/api/client/payment-method")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken))
                        .contentType(MediaType.APPLICATION_JSON).content(CARD_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.method.maskedNumber").value("**** 1111"));

        Integer requestId = json(mvc.perform(post("/api/client/change-requests")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken))
                        .contentType(MediaType.APPLICATION_JSON).content(IBAN_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn(), "$.id");

        String boToken = loginBo(mvc);

        mvc.perform(get("/api/bo/change-requests/{id}", requestId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(boToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentMethod.type").value("CARD"))
                .andExpect(jsonPath("$.proposedMethod.type").value("BANK_ACCOUNT"));

        mvc.perform(post("/api/bo/change-requests/{id}/approve", requestId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(boToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"comment\": \"Verified\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewerName").value("Crego Admin"));

        mvc.perform(get("/api/client/payment-method")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method.type").value("BANK_ACCOUNT"))
                .andExpect(jsonPath("$.method.maskedNumber").value("LV** **** **** 5001"));

        mvc.perform(get("/api/client/change-requests")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("APPROVED"))
                .andExpect(jsonPath("$[0].reviewComment").value("Verified"));

        // The client is also told by email
        SentEmail email = mail.lastSentTo(client.email()).orElseThrow();
        assertThat(email.subject()).isEqualTo("Your payment method change was approved");
        assertThat(email.text()).contains("Comment: Verified");

        // Deciding the same request again is a conflict
        mvc.perform(post("/api/bo/change-requests/{id}/reject", requestId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(boToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\": \"Too late\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectedChangeKeepsTheCurrentMethodAndShowsTheReason() throws Exception {
        RegisteredClient client = registerNewClient(mvc, mail);
        String clientToken = client.token();
        addCard(mvc, clientToken);
        Integer requestId = requestIbanChange(mvc, clientToken);

        mvc.perform(post("/api/bo/change-requests/{id}/reject", requestId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginBo(mvc)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\": \"IBAN owner does not match\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mvc.perform(get("/api/client/payment-method")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
                .andExpect(jsonPath("$.method.type").value("CARD"));
        mvc.perform(get("/api/client/change-requests")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
                .andExpect(jsonPath("$[0].status").value("REJECTED"))
                .andExpect(jsonPath("$[0].reviewComment").value("IBAN owner does not match"));

        SentEmail email = mail.lastSentTo(client.email()).orElseThrow();
        assertThat(email.subject()).isEqualTo("Your payment method change was rejected");
        assertThat(email.text()).contains("Reason: IBAN owner does not match");
    }
}
