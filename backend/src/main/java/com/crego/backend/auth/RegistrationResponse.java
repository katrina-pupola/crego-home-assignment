package com.crego.backend.auth;

/** Returned after registering; no token yet, because the email must be confirmed first. */
public record RegistrationResponse(String email) {
}
