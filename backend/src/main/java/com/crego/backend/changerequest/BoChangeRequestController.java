package com.crego.backend.changerequest;

import com.crego.backend.auth.CurrentUser;
import com.crego.backend.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bo/change-requests")
@RequiredArgsConstructor
public class BoChangeRequestController {

    private final ChangeRequestReviewService reviewService;

    @GetMapping
    public PageResponse<BoChangeRequestSummary> list(@RequestParam(required = false) ChangeRequestStatus status,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return reviewService.list(status, page, size);
    }

    @GetMapping("/{id}")
    public BoChangeRequestDetail get(@PathVariable long id) {
        return reviewService.get(id);
    }

    @PostMapping("/{id}/approve")
    public BoChangeRequestDetail approve(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                         @Valid @RequestBody(required = false) ApproveRequest request) {
        return reviewService.approve(id, CurrentUser.id(jwt), request == null ? null : request.comment());
    }

    @PostMapping("/{id}/reject")
    public BoChangeRequestDetail reject(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                        @Valid @RequestBody RejectRequest request) {
        return reviewService.reject(id, CurrentUser.id(jwt), request.reason());
    }
}
