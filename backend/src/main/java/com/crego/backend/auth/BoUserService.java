package com.crego.backend.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class BoUserService {

    private final BoUserRepository boUserRepository;

    @Transactional(readOnly = true)
    public BoUserResponse getProfile(long userId) {
        return boUserRepository.findById(userId)
                .map(BoUserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff user not found"));
    }
}
