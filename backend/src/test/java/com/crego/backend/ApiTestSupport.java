package com.crego.backend;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Login and registration helpers shared by the integration tests. */
public final class ApiTestSupport {

    public static final String CARD_JSON = """
            {"type": "CARD", "cardNumber": "4111 1111 1111 1111", "cardHolder": "Test Client", "cardExpiry": "12/40"}
            """;
    public static final String IBAN_JSON = """
            {"type": "BANK_ACCOUNT", "iban": "LV80BANK0000435195001"}
            """;

    private ApiTestSupport() {
    }

    public record RegisteredClient(String email, String token) {
    }

    /** Registers a client with a unique email, so tests never depend on each other's data. */
    public static String registerUnverifiedClient(MockMvc mvc) throws Exception {
        String email = "test-" + UUID.randomUUID() + "@crego.test";
        String body = """
                {"fullName": "Test Client", "email": "%s", "password": "Secret-Pass-123"}
                """.formatted(email);
        mvc.perform(post("/api/auth/client/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        return email;
    }

    /** Registers a client and clicks the link from the verification email, like a real user. */
    public static RegisteredClient registerNewClient(MockMvc mvc, RecordingMailSender mail) throws Exception {
        String email = registerUnverifiedClient(mvc);
        String token = token(verify(mvc, mail.verificationToken(email)).andExpect(status().isOk()).andReturn());
        return new RegisteredClient(email, token);
    }

    public static ResultActions verify(MockMvc mvc, String verificationToken) throws Exception {
        return mvc.perform(post("/api/auth/client/verify").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"%s\"}".formatted(verificationToken)));
    }

    public static String loginBo(MockMvc mvc) throws Exception {
        return login(mvc, "/api/auth/bo/login", "admin@crego.test", "Admin-Review-2026");
    }

    public static String loginDemoClient(MockMvc mvc) throws Exception {
        return login(mvc, "/api/auth/client/login", "demo@crego.test", "Demo-Client-2026");
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }

    public static <T> T json(MvcResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }

    private static String login(MockMvc mvc, String url, String email, String password) throws Exception {
        String body = """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
        return token(mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn());
    }

    private static String token(MvcResult result) throws Exception {
        return json(result, "$.token");
    }
}
