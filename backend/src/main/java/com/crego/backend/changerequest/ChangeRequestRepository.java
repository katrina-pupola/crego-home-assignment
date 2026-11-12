package com.crego.backend.changerequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Long> {

    List<ChangeRequest> findByClientIdOrderByCreatedAtDesc(Long clientId);

    boolean existsByClientIdAndStatus(Long clientId, ChangeRequestStatus status);

    // @EntityGraph loads the client in the same query, avoiding one extra query per row
    @EntityGraph(attributePaths = "client")
    Page<ChangeRequest> findByStatus(ChangeRequestStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "client")
    Page<ChangeRequest> findAll(Pageable pageable);
}
