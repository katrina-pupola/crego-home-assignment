package com.crego.backend;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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

    /** Registers a client with a unique email, so tests never depend on each other's data. */
    public static String registerNewClient(MockMvc mvc) throws Exception {
        String email = "test-" + UUID.randomUUID() + "@crego.test";
        String body = """
                {"fullName": "Test Client", "email": "%s", "password": "Secret-Pass-123"}
                """.formatted(email);
        return token(mvc.perform(post("/api/auth/client/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn());
    }

    public static String loginBo(MockMvc mvc) throws Exception {
        return login(mvc, "/api/auth/bo/login", "admin@crego.test", "admin123");
    }

    public static String loginDemoClient(MockMvc mvc) throws Exception {
        return login(mvc, "/api/auth/client/login", "demo@crego.test", "demo123");
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
