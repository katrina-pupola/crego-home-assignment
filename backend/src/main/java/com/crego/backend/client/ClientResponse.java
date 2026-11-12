package com.crego.backend.client;

import java.time.Instant;

public record ClientResponse(Long id, String email, String fullName, Instant createdAt) {

    static ClientResponse from(Client client) {
        return new ClientResponse(client.getId(), client.getEmail(), client.getFullName(), client.getCreatedAt());
    }
}
