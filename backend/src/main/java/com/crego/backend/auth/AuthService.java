package com.crego.backend.auth;

import com.crego.backend.client.Client;
import com.crego.backend.client.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    static final String EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";

    private final ClientRepository clientRepository;
    private final BoUserRepository boUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailVerificationService emailVerificationService;

    // The account starts unverified; the client logs in by clicking the link in the email
    @Transactional
    public RegistrationResponse registerClient(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (clientRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        Client client = new Client();
        client.setEmail(email);
        client.setFullName(request.fullName().trim());
        client.setPasswordHash(passwordEncoder.encode(request.password()));
        clientRepository.save(client);

        emailVerificationService.sendVerification(client);
        return new RegistrationResponse(email);
    }

    @Transactional(readOnly = true)
    public TokenResponse loginClient(LoginRequest request) {
        Client client = clientRepository.findByEmail(normalizeEmail(request.email()))
                .filter(c -> passwordEncoder.matches(request.password(), c.getPasswordHash()))
                .orElseThrow(AuthService::invalidCredentials);
        // Checked after the password, so this answer never reveals anything to someone guessing
        if (!client.isEmailVerified()) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                    "Please confirm your email address first. Check your inbox for the link.");
            problem.setProperty("code", EMAIL_NOT_VERIFIED);
            throw new ErrorResponseException(HttpStatus.FORBIDDEN, problem, null);
        }
        return tokenService.issue(client.getId(), Role.CLIENT);
    }

    @Transactional(readOnly = true)
    public TokenResponse loginBoUser(LoginRequest request) {
        BoUser user = boUserRepository.findByEmail(normalizeEmail(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(AuthService::invalidCredentials);
        return tokenService.issue(user.getId(), Role.BO);
    }

    // Same error for unknown email and wrong password, so registered emails can't be discovered
    private static ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
