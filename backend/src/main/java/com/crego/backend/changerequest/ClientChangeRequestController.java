package com.crego.backend.changerequest;

import com.crego.backend.auth.CurrentUser;
import com.crego.backend.paymentmethod.PaymentMethodRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/client/change-requests")
@RequiredArgsConstructor
public class ClientChangeRequestController {

    private final ChangeRequestService changeRequestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChangeRequestResponse create(@AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody PaymentMethodRequest request) {
        return changeRequestService.create(CurrentUser.id(jwt), request);
    }

    @GetMapping
    public List<ChangeRequestResponse> listOwn(@AuthenticationPrincipal Jwt jwt) {
        return changeRequestService.listOwn(CurrentUser.id(jwt));
    }

    @PostMapping("/{id}/cancel")
    public ChangeRequestResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return changeRequestService.cancel(CurrentUser.id(jwt), id);
    }
}
