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
import java.util.Optional;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class AuthService {

    static final String EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";
    // BCrypt hash of a random value that is no one's password
    private static final String DUMMY_HASH = "$2a$10$ltHjD11Nx8pTIipOBILgyOd1NSAob9fRE5qzQqoi1sL6jf/jQAAla";

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
        Client client = checkPassword(clientRepository.findByEmail(normalizeEmail(request.email())),
                Client::getPasswordHash, request.password())
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
        BoUser user = checkPassword(boUserRepository.findByEmail(normalizeEmail(request.email())),
                BoUser::getPasswordHash, request.password())
                .orElseThrow(AuthService::invalidCredentials);
        return tokenService.issue(user.getId(), Role.BO);
    }

    // Runs BCrypt even for unknown emails, so response time does not reveal which emails are registered
    private <T> Optional<T> checkPassword(Optional<T> user, Function<T, String> hashOf, String rawPassword) {
        if (user.isEmpty()) {
            passwordEncoder.matches(rawPassword, DUMMY_HASH);
            return Optional.empty();
        }
        return user.filter(u -> passwordEncoder.matches(rawPassword, hashOf.apply(u)));
    }

    // Same error for unknown email and wrong password, so registered emails can't be discovered
    private static ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
