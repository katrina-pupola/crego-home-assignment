package com.crego.backend.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bo")
@RequiredArgsConstructor
public class BoUserController {

    private final BoUserService boUserService;

    @GetMapping("/me")
    public BoUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return boUserService.getProfile(CurrentUser.id(jwt));
    }
}
