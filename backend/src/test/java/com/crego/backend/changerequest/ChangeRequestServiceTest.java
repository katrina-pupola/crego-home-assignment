package com.crego.backend.changerequest;

import com.crego.backend.client.Client;
import com.crego.backend.paymentmethod.CardBrand;
import com.crego.backend.paymentmethod.MethodData;
import com.crego.backend.paymentmethod.PaymentMethod;
import com.crego.backend.paymentmethod.PaymentMethodRepository;
import com.crego.backend.paymentmethod.PaymentMethodRequest;
import com.crego.backend.paymentmethod.PaymentMethodStatus;
import com.crego.backend.paymentmethod.PaymentMethodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeRequestServiceTest {

    private static final long CLIENT_ID = 1L;
    private static final PaymentMethodRequest NEW_IBAN =
            new PaymentMethodRequest(PaymentMethodType.BANK_ACCOUNT, null, null, null, "lv80 bank 0000 4351 9500 1");

    @Mock
    private ChangeRequestRepository changeRequestRepository;
    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @InjectMocks
    private ChangeRequestService service;

    private Client client;
    private PaymentMethod activeMethod;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setId(CLIENT_ID);

        activeMethod = new PaymentMethod();
        activeMethod.setClient(client);
        activeMethod.setStatus(PaymentMethodStatus.ACTIVE);
        activeMethod.setDetails(new MethodData(PaymentMethodType.CARD, "1111", CardBrand.VISA, "Anna Ozola", "12/28", null));
    }

    @Test
    void createStoresPendingRequestWithCurrentAndProposedMethod() {
        when(paymentMethodRepository.findByClientIdAndStatus(CLIENT_ID, PaymentMethodStatus.ACTIVE))
                .thenReturn(Optional.of(activeMethod));
        when(changeRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(CLIENT_ID, NEW_IBAN);

        ArgumentCaptor<ChangeRequest> saved = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(ChangeRequestStatus.PENDING);
        assertThat(saved.getValue().getCurrentMethod()).isSameAs(activeMethod);
        assertThat(saved.getValue().getNewIban()).isEqualTo("LV80BANK0000435195001");
    }

    @Test
    void onlyOnePendingRequestAtATime() {
        when(paymentMethodRepository.findByClientIdAndStatus(CLIENT_ID, PaymentMethodStatus.ACTIVE))
                .thenReturn(Optional.of(activeMethod));
        when(changeRequestRepository.existsByClientIdAndStatus(CLIENT_ID, ChangeRequestStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> service.create(CLIENT_ID, NEW_IBAN))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(changeRequestRepository, never()).save(any());
    }

    @Test
    void changeRequiresAnExistingMethod() {
        when(paymentMethodRepository.findByClientIdAndStatus(CLIENT_ID, PaymentMethodStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(CLIENT_ID, NEW_IBAN))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(changeRequestRepository, never()).save(any());
    }

    @Test
    void clientCannotCancelSomeoneElsesRequest() {
        Client otherClient = new Client();
        otherClient.setId(2L);
        ChangeRequest othersRequest = new ChangeRequest();
        othersRequest.setClient(otherClient);
        othersRequest.setStatus(ChangeRequestStatus.PENDING);
        when(changeRequestRepository.findById(7L)).thenReturn(Optional.of(othersRequest));

        assertThatThrownBy(() -> service.cancel(CLIENT_ID, 7L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThat(othersRequest.getStatus()).isEqualTo(ChangeRequestStatus.PENDING);
    }
}
