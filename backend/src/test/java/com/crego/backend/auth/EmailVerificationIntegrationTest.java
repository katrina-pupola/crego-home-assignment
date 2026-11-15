package com.crego.backend.auth;

import com.crego.backend.IntegrationTest;
import com.crego.backend.RecordingMailSender;
import com.crego.backend.RecordingMailSender.SentEmail;
import com.crego.backend.client.ClientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.time.Instant;

import static com.crego.backend.ApiTestSupport.bearer;
import static com.crego.backend.ApiTestSupport.json;
import static com.crego.backend.ApiTestSupport.loginDemoClient;
import static com.crego.backend.ApiTestSupport.registerUnverifiedClient;
import static com.crego.backend.ApiTestSupport.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class EmailVerificationIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RecordingMailSender mail;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private EmailVerificationService verificationService;

    @Autowired
    private ClientRepository clientRepository;

    @Test
    void clientCanOnlyLogInAfterClickingTheEmailLink() throws Exception {
        String email = registerUnverifiedClient(mvc);

        // Correct password, but the email is not confirmed yet
        mvc.perform(login(email))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

        SentEmail message = mail.lastSentTo(email).orElseThrow();
        assertThat(message.subject()).isEqualTo("Confirm your email address");
        assertThat(message.text()).contains("/verify?token=");
        assertThat(message.html()).contains("Confirm email address").contains("/verify?token=");

        String verificationToken = mail.verificationToken(email);
        String jwt = json(verify(mvc, verificationToken).andExpect(status().isOk()).andReturn(), "$.token");

        mvc.perform(get("/api/client/me").header(HttpHeaders.AUTHORIZATION, bearer(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
        mvc.perform(login(email)).andExpect(status().isOk());

        // The link works only once
        verify(mvc, verificationToken).andExpect(status().isBadRequest());
    }

    @Test
    void expiredLinkIsRejected() throws Exception {
        String email = registerUnverifiedClient(mvc);
        String verificationToken = mail.verificationToken(email);

        EmailVerificationToken stored = tokenRepository
                .findByTokenHash(EmailVerificationService.hash(verificationToken)).orElseThrow();
        stored.setExpiresAt(Instant.now().minusSeconds(1));
        tokenRepository.save(stored);

        verify(mvc, verificationToken).andExpect(status().isBadRequest());
    }

    @Test
    void newLinkMakesTheOldOneStopWorking() throws Exception {
        String email = registerUnverifiedClient(mvc);
        String oldToken = mail.verificationToken(email);

        verificationService.sendVerification(clientRepository.findByEmail(email).orElseThrow());
        String newToken = mail.verificationToken(email);

        assertThat(newToken).isNotEqualTo(oldToken);
        verify(mvc, oldToken).andExpect(status().isBadRequest());
        verify(mvc, newToken).andExpect(status().isOk());
    }

    @Test
    void onlyTheHashOfTheTokenIsStored() throws Exception {
        String email = registerUnverifiedClient(mvc);
        String verificationToken = mail.verificationToken(email);

        assertThat(tokenRepository.findAll())
                .extracting(EmailVerificationToken::getTokenHash)
                .doesNotContain(verificationToken)
                .contains(EmailVerificationService.hash(verificationToken));
    }

    @Test
    void resendIsLimitedAndDoesNotRevealUnknownEmails() throws Exception {
        String email = registerUnverifiedClient(mvc);

        // Within a minute of the first email, no second one is sent
        mvc.perform(resend(email)).andExpect(status().isAccepted());
        assertThat(mail.sentTo(email)).hasSize(1);

        // Unknown address: same response, nothing sent
        mvc.perform(resend("nobody@crego.test")).andExpect(status().isAccepted());
        assertThat(mail.sentTo("nobody@crego.test")).isEmpty();
    }

    @Test
    void seededDemoClientIsAlreadyVerified() throws Exception {
        loginDemoClient(mvc); // expects 200
    }

    private static RequestBuilder login(String email) {
        return post("/api/auth/client/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"password\": \"Secret-Pass-123\"}".formatted(email));
    }

    private static RequestBuilder resend(String email) {
        return post("/api/auth/client/resend-verification").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\"}".formatted(email));
    }
}
