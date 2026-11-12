package com.crego.backend.auth;

import java.time.Instant;

public record TokenResponse(String token, Instant expiresAt) {
}
