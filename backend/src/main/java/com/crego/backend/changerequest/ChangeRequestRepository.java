package com.crego.backend.changerequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Long> {

    List<ChangeRequest> findByClientIdOrderByCreatedAtDesc(Long clientId);

    boolean existsByClientIdAndStatus(Long clientId, ChangeRequestStatus status);

    Page<ChangeRequest> findByStatus(ChangeRequestStatus status, Pageable pageable);
}
