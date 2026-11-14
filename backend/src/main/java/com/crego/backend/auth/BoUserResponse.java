package com.crego.backend.auth;

public record BoUserResponse(Long id, String email, String fullName) {

    static BoUserResponse from(BoUser user) {
        return new BoUserResponse(user.getId(), user.getEmail(), user.getFullName());
    }
}
