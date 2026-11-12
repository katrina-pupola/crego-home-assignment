package com.crego.backend.paymentmethod;

import com.crego.backend.auth.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client/payment-method")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping
    public PaymentMethodResponse getActive(@AuthenticationPrincipal Jwt jwt) {
        return paymentMethodService.getActive(CurrentUser.id(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentMethodResponse addFirst(@AuthenticationPrincipal Jwt jwt,
                                          @Valid @RequestBody PaymentMethodRequest request) {
        return paymentMethodService.addFirst(CurrentUser.id(jwt), request);
    }
}
