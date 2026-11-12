package com.crego.backend.auth;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@SecurityRequirements // public endpoints: no token needed in Swagger UI
public class AuthController {

    private final AuthService authService;

    @PostMapping("/client/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse registerClient(@Valid @RequestBody RegisterRequest request) {
        return authService.registerClient(request);
    }

    @PostMapping("/client/login")
    public TokenResponse loginClient(@Valid @RequestBody LoginRequest request) {
        return authService.loginClient(request);
    }

    @PostMapping("/bo/login")
    public TokenResponse loginBoUser(@Valid @RequestBody LoginRequest request) {
        return authService.loginBoUser(request);
    }
}
