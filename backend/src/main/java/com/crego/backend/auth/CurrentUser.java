package com.crego.backend.auth;

import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUser {

    private CurrentUser() {
    }

    /** The logged-in user's id, taken from the token subject (never from the request). */
    public static long id(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
