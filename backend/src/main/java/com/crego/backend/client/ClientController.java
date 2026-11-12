package com.crego.backend.client;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    // The client id comes from the token's subject, never from the request
    @GetMapping("/me")
    public ClientResponse me(@AuthenticationPrincipal Jwt jwt) {
        return clientService.getProfile(Long.parseLong(jwt.getSubject()));
    }
}
