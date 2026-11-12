package com.crego.backend.auth;

import com.crego.backend.client.Client;
import com.crego.backend.client.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ClientRepository clientRepository;
    private final BoUserRepository boUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional
    public TokenResponse registerClient(RegisterRequest request) {
        String email = normalize(request.email());
        if (clientRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        Client client = new Client();
        client.setEmail(email);
        client.setFullName(request.fullName().trim());
        client.setPasswordHash(passwordEncoder.encode(request.password()));
        clientRepository.save(client);

        return tokenService.issue(client.getId(), Role.CLIENT);
    }

    @Transactional(readOnly = true)
    public TokenResponse loginClient(LoginRequest request) {
        Client client = clientRepository.findByEmail(normalize(request.email()))
                .filter(c -> passwordEncoder.matches(request.password(), c.getPasswordHash()))
                .orElseThrow(AuthService::invalidCredentials);
        return tokenService.issue(client.getId(), Role.CLIENT);
    }

    @Transactional(readOnly = true)
    public TokenResponse loginBoUser(LoginRequest request) {
        BoUser user = boUserRepository.findByEmail(normalize(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(AuthService::invalidCredentials);
        return tokenService.issue(user.getId(), Role.BO);
    }

    // Same error for unknown email and wrong password, so registered emails can't be discovered
    private static ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
