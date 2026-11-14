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
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/client/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse registerClient(@Valid @RequestBody RegisterRequest request) {
        return authService.registerClient(request);
    }

    @PostMapping("/client/verify")
    public TokenResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return emailVerificationService.verify(request.token());
    }

    // Always 202, whether or not the email exists
    @PostMapping("/client/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        emailVerificationService.resend(request.email());
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
