package com.crego.backend.paymentmethod;

import com.crego.backend.client.Client;
import com.crego.backend.client.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentMethodServiceTest {

    private static final long CLIENT_ID = 1L;
    private static final PaymentMethodRequest VISA =
            new PaymentMethodRequest(PaymentMethodType.CARD, "4111 1111 1111 1111", " Anna Ozola ", "12/40", null);

    @Mock
    private PaymentMethodRepository paymentMethodRepository;
    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private PaymentMethodService service;

    @Test
    void firstMethodIsActiveAndStoresOnlyLastFourDigits() {
        when(clientRepository.getReferenceById(CLIENT_ID)).thenReturn(new Client());
        when(paymentMethodRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentMethodResponse response = service.addFirst(CLIENT_ID, VISA);

        ArgumentCaptor<PaymentMethod> saved = ArgumentCaptor.forClass(PaymentMethod.class);
        verify(paymentMethodRepository).save(saved.capture());
        PaymentMethod method = saved.getValue();
        assertThat(method.getStatus()).isEqualTo(PaymentMethodStatus.ACTIVE);
        assertThat(method.getCardLast4()).isEqualTo("1111");
        assertThat(method.getCardBrand()).isEqualTo(CardBrand.VISA);
        assertThat(method.getCardHolder()).isEqualTo("Anna Ozola");
        assertThat(response.method().maskedNumber()).isEqualTo("**** 1111");
    }

    @Test
    void secondDirectAddIsRejected() {
        when(paymentMethodRepository.existsByClientIdAndStatus(CLIENT_ID, PaymentMethodStatus.ACTIVE)).thenReturn(true);

        assertStatus(() -> service.addFirst(CLIENT_ID, VISA), HttpStatus.CONFLICT);
        verify(paymentMethodRepository, never()).save(any());
    }

    private static void assertStatus(Runnable action, HttpStatus expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(expected));
    }
}
