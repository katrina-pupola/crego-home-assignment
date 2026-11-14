package com.crego.backend.auth;

import com.crego.backend.client.Client;
import com.crego.backend.client.ClientRepository;
import com.crego.backend.notification.AppMailProperties;
import com.crego.backend.notification.VerificationEmailRequested;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final ClientRepository clientRepository;
    private final TokenService tokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final AppMailProperties mailProperties;

    /** Creates a new single-use link (older ones stop working) and emails it after commit. */
    @Transactional
    public void sendVerification(Client client) {
        Instant now = Instant.now();
        tokenRepository.invalidateUnused(client.getId(), now);

        String token = newToken();
        EmailVerificationToken entity = new EmailVerificationToken();
        entity.setClient(client);
        entity.setTokenHash(hash(token));
        entity.setExpiresAt(now.plus(mailProperties.verificationTtl()));
        tokenRepository.save(entity);

        eventPublisher.publishEvent(new VerificationEmailRequested(client.getEmail(), client.getFullName(), token));
    }

    /** Marks the email as verified and logs the client in. */
    @Transactional
    public TokenResponse verify(String token) {
        Instant now = Instant.now();
        EmailVerificationToken entity = tokenRepository.findByTokenHash(hash(token))
                .filter(t -> t.isUsable(now))
                .orElseThrow(EmailVerificationService::invalidLink);
        // Conditional update, so two simultaneous clicks cannot both use the same link
        if (tokenRepository.markUsed(entity.getId(), now) == 0) {
            throw invalidLink();
        }

        Client client = entity.getClient();
        if (!client.isEmailVerified()) {
            client.setEmailVerifiedAt(now);
        }
        return tokenService.issue(client.getId(), Role.CLIENT);
    }

    // Behaves the same for unknown emails, so the endpoint does not reveal who is registered
    @Transactional
    public void resend(String email) {
        clientRepository.findByEmail(AuthService.normalizeEmail(email))
                .filter(client -> !client.isEmailVerified())
                .filter(client -> !sentRecently(client))
                .ifPresent(this::sendVerification);
    }

    private static ResponseStatusException invalidLink() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "This link is invalid or has expired");
    }

    private boolean sentRecently(Client client) {
        Instant limit = Instant.now().minus(RESEND_COOLDOWN);
        return tokenRepository.findFirstByClientIdOrderByCreatedAtDesc(client.getId())
                .map(t -> t.getCreatedAt().isAfter(limit))
                .orElse(false);
    }

    // 256 random bits, URL-safe so it can go straight into the link
    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // A fast hash is enough here: the token is random and long, unlike a user's password
    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
