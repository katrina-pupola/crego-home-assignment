package com.crego.backend.changerequest;

import com.crego.backend.auth.BoUser;
import com.crego.backend.auth.BoUserRepository;
import com.crego.backend.client.Client;
import com.crego.backend.notification.ChangeRequestDecided;
import com.crego.backend.paymentmethod.CardBrand;
import com.crego.backend.paymentmethod.MethodData;
import com.crego.backend.paymentmethod.PaymentMethod;
import com.crego.backend.paymentmethod.PaymentMethodRepository;
import com.crego.backend.paymentmethod.PaymentMethodStatus;
import com.crego.backend.paymentmethod.PaymentMethodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeRequestReviewServiceTest {

    private static final long REQUEST_ID = 5L;
    private static final long CLIENT_ID = 1L;
    private static final long REVIEWER_ID = 10L;

    @Mock
    private ChangeRequestRepository changeRequestRepository;
    @Mock
    private PaymentMethodRepository paymentMethodRepository;
    @Mock
    private BoUserRepository boUserRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChangeRequestReviewService service;

    private Client client;
    private BoUser reviewer;
    private PaymentMethod currentMethod;
    private ChangeRequest changeRequest;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setId(CLIENT_ID);
        client.setFullName("Anna Ozola");
        client.setEmail("anna@crego.test");

        reviewer = new BoUser();
        reviewer.setId(REVIEWER_ID);
        reviewer.setFullName("Crego Admin");

        currentMethod = new PaymentMethod();
        currentMethod.setClient(client);
        currentMethod.setStatus(PaymentMethodStatus.ACTIVE);
        currentMethod.setDetails(new MethodData(PaymentMethodType.CARD, "1111", CardBrand.VISA, "Anna Ozola", "12/28", null));

        changeRequest = new ChangeRequest();
        changeRequest.setId(REQUEST_ID);
        changeRequest.setClient(client);
        changeRequest.setCurrentMethod(currentMethod);
        changeRequest.setStatus(ChangeRequestStatus.PENDING);
        changeRequest.setProposed(new MethodData(PaymentMethodType.BANK_ACCOUNT, null, null, null, null, "LV80BANK0000435195001"));
    }

    @Test
    void approveReplacesCurrentMethodAndActivatesProposedOne() {
        when(changeRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(changeRequest));
        when(paymentMethodRepository.findByClientIdAndStatus(CLIENT_ID, PaymentMethodStatus.ACTIVE))
                .thenReturn(Optional.of(currentMethod));
        when(boUserRepository.getReferenceById(REVIEWER_ID)).thenReturn(reviewer);

        BoChangeRequestDetail result = service.approve(REQUEST_ID, REVIEWER_ID, "  Verified  ");

        assertThat(currentMethod.getStatus()).isEqualTo(PaymentMethodStatus.REPLACED);

        // The old method must be flushed as REPLACED before the new ACTIVE one is inserted
        ArgumentCaptor<PaymentMethod> saved = ArgumentCaptor.forClass(PaymentMethod.class);
        InOrder order = inOrder(paymentMethodRepository);
        order.verify(paymentMethodRepository).flush();
        order.verify(paymentMethodRepository).save(saved.capture());

        PaymentMethod newMethod = saved.getValue();
        assertThat(newMethod.getStatus()).isEqualTo(PaymentMethodStatus.ACTIVE);
        assertThat(newMethod.getClient()).isSameAs(client);
        assertThat(newMethod.getDetails()).isEqualTo(changeRequest.getProposed());

        assertThat(changeRequest.getStatus()).isEqualTo(ChangeRequestStatus.APPROVED);
        assertThat(changeRequest.getReviewer()).isSameAs(reviewer);
        assertThat(changeRequest.getReviewComment()).isEqualTo("Verified");
        assertThat(changeRequest.getReviewedAt()).isNotNull();
        assertThat(result.reviewerName()).isEqualTo("Crego Admin");
        verify(eventPublisher).publishEvent(
                new ChangeRequestDecided("anna@crego.test", "Anna Ozola", true, "Verified"));
    }

    @Test
    void rejectRecordsReasonAndLeavesPaymentMethodsUntouched() {
        when(changeRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(changeRequest));
        when(boUserRepository.getReferenceById(REVIEWER_ID)).thenReturn(reviewer);

        service.reject(REQUEST_ID, REVIEWER_ID, "Name does not match");

        assertThat(changeRequest.getStatus()).isEqualTo(ChangeRequestStatus.REJECTED);
        assertThat(changeRequest.getReviewComment()).isEqualTo("Name does not match");
        assertThat(currentMethod.getStatus()).isEqualTo(PaymentMethodStatus.ACTIVE);
        verifyNoInteractions(paymentMethodRepository);
    }

    @ParameterizedTest
    @EnumSource(value = ChangeRequestStatus.class, names = {"APPROVED", "REJECTED", "CANCELLED"})
    void cannotDecideARequestTwice(ChangeRequestStatus finalStatus) {
        changeRequest.setStatus(finalStatus);
        when(changeRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(changeRequest));

        assertStatus(() -> service.approve(REQUEST_ID, REVIEWER_ID, null), HttpStatus.CONFLICT);
        assertStatus(() -> service.reject(REQUEST_ID, REVIEWER_ID, "reason"), HttpStatus.CONFLICT);
        assertThat(changeRequest.getStatus()).isEqualTo(finalStatus);
        verifyNoInteractions(paymentMethodRepository);
    }

    @Test
    void cardThatExpiredWhileWaitingCannotBeApproved() {
        changeRequest.setProposed(new MethodData(PaymentMethodType.CARD, "4444", CardBrand.MASTERCARD,
                "Anna Ozola", "01/20", null));
        when(changeRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(changeRequest));

        assertStatus(() -> service.approve(REQUEST_ID, REVIEWER_ID, null), HttpStatus.CONFLICT);
        assertThat(changeRequest.getStatus()).isEqualTo(ChangeRequestStatus.PENDING);
        verifyNoInteractions(paymentMethodRepository);
    }

    @Test
    void approvingUnknownRequestReturnsNotFound() {
        when(changeRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.empty());

        assertStatus(() -> service.approve(REQUEST_ID, REVIEWER_ID, null), HttpStatus.NOT_FOUND);
    }

    private static void assertStatus(Runnable action, HttpStatus expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(expected));
    }
}
