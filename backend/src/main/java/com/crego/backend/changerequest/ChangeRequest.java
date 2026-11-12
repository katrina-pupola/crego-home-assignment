package com.crego.backend.changerequest;

import com.crego.backend.auth.BoUser;
import com.crego.backend.client.Client;
import com.crego.backend.paymentmethod.CardBrand;
import com.crego.backend.paymentmethod.PaymentMethod;
import com.crego.backend.paymentmethod.PaymentMethodType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id")
    private Client client;

    // Method that was active when the request was made
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_method_id")
    private PaymentMethod currentMethod;

    // Proposed new method
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethodType newType;

    @Column(name = "new_card_last4")
    private String newCardLast4;

    @Enumerated(EnumType.STRING)
    private CardBrand newCardBrand;

    private String newCardHolder;

    private String newCardExpiry;

    private String newIban;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeRequestStatus status;

    // Review outcome, set when BO staff decide
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private BoUser reviewer;

    private String reviewComment;

    private Instant reviewedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Optimistic locking: a save fails if another transaction changed the row first
    @Version
    private long version;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
