package com.eventapp.sharedutils.security.keycloak;

import com.eventapp.sharedutils.exceptions.domain.UnauthorizedException;
import com.eventapp.sharedutils.security.ISecurityService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public class SecurityServiceImpl implements ISecurityService {

    @Override
    public String getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt)
            return jwt.getSubject(); // Keycloak 'sub'

        throw new UnauthorizedException();
    }

    @Override
    public String getUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt)
            return jwt.getClaimAsString("email");

        throw new UnauthorizedException();
    }
}