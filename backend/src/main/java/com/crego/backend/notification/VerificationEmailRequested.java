package com.crego.backend.notification;

/** Published when a client needs a verification link; the token is only kept in memory for the email. */
public record VerificationEmailRequested(String email, String fullName, String token) {
}
