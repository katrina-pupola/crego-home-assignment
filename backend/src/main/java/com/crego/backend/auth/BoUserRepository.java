package com.crego.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoUserRepository extends JpaRepository<BoUser, Long> {

    Optional<BoUser> findByEmail(String email);
}
