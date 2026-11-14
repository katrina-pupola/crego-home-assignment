package com.crego.backend.auth;

import com.crego.backend.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.crego.backend.ApiTestSupport.bearer;
import static com.crego.backend.ApiTestSupport.loginBo;
import static com.crego.backend.ApiTestSupport.loginDemoClient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void clientTokenCannotUseBoEndpoints() throws Exception {
        mvc.perform(get("/api/bo/change-requests").header(HttpHeaders.AUTHORIZATION, bearer(loginDemoClient(mvc))))
                .andExpect(status().isForbidden());
    }

    @Test
    void boTokenCannotUseClientEndpoints() throws Exception {
        mvc.perform(get("/api/client/me").header(HttpHeaders.AUTHORIZATION, bearer(loginBo(mvc))))
                .andExpect(status().isForbidden());
    }

    @Test
    void clientTokenCanReadOwnProfile() throws Exception {
        mvc.perform(get("/api/client/me").header(HttpHeaders.AUTHORIZATION, bearer(loginDemoClient(mvc))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("demo@crego.test"));
    }

    @Test
    void boTokenCanReadOwnProfile() throws Exception {
        mvc.perform(get("/api/bo/me").header(HttpHeaders.AUTHORIZATION, bearer(loginBo(mvc))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Crego Admin"));
    }

    @Test
    void clientTokenCannotReadBoProfile() throws Exception {
        mvc.perform(get("/api/bo/me").header(HttpHeaders.AUTHORIZATION, bearer(loginDemoClient(mvc))))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestWithoutTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/api/client/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/bo/change-requests")).andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedTokenIsUnauthorized() throws Exception {
        String token = loginDemoClient(mvc);
        mvc.perform(get("/api/client/me").header(HttpHeaders.AUTHORIZATION, bearer(token + "x")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffCredentialsDoNotWorkOnClientLogin() throws Exception {
        mvc.perform(post("/api/auth/client/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"admin@crego.test\", \"password\": \"admin123\"}"))
                .andExpect(status().isUnauthorized());
    }
}
