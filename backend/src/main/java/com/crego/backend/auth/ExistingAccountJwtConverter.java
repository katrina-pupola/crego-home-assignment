package com.crego.backend.auth;

import com.crego.backend.client.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

/**
 * Turns a valid token into the logged-in user with their role, but only if that account still exists.
 * A token for a deleted account (or one issued by another environment with the same secret) gets 401,
 * so the frontend logs the user out instead of failing later with confusing errors.
 */
@Component
@RequiredArgsConstructor
public class ExistingAccountJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final ClientRepository clientRepository;
    private final BoUserRepository boUserRepository;
    private final JwtAuthenticationConverter roleConverter = roleConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        long userId = CurrentUser.id(jwt);
        String role = jwt.getClaimAsString(SecurityConfig.ROLE_CLAIM);
        boolean exists = Role.CLIENT.name().equals(role) ? clientRepository.existsById(userId)
                : Role.BO.name().equals(role) && boUserRepository.existsById(userId);
        if (!exists) {
            throw new InvalidBearerTokenException("The account for this token no longer exists");
        }
        return roleConverter.convert(jwt);
    }

    // Maps the token's "role" claim (e.g. CLIENT) to the authority ROLE_CLIENT used by hasRole()
    private static JwtAuthenticationConverter roleConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(SecurityConfig.ROLE_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
