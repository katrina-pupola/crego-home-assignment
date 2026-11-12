package com.crego.backend.paymentmethod;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    Optional<PaymentMethod> findByClientIdAndStatus(Long clientId, PaymentMethodStatus status);

    boolean existsByClientIdAndStatus(Long clientId, PaymentMethodStatus status);
}
