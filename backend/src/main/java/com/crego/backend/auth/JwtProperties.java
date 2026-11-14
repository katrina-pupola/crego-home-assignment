package com.crego.backend.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration ttl) {

    // HS256 needs a key of at least 256 bits; fail at startup instead of at the first login
    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("app.jwt.secret (JWT_SECRET) must be at least 32 bytes long");
        }
    }
}
