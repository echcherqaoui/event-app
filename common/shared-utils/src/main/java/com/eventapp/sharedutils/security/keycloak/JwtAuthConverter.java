package com.eventapp.sharedutils.security.keycloak;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess == null || realmAccess.get("roles") == null)
            return Set.of();

        Object rolesObject = realmAccess.get("roles");

        if (rolesObject instanceof Collection<?> roles)
            return roles.stream()
                  .filter(String.class::isInstance) // Only keep actual strings
                  .map(role -> new SimpleGrantedAuthority("ROLE_" + ((String) role).toUpperCase()))
                  .collect(Collectors.toSet());

        return Set.of();
    }

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        Stream<GrantedAuthority> grantedAuthorityStream = Optional.of(
              defaultConverter.convert(jwt)
        ).stream()
        .flatMap(Collection::stream);

        Collection<GrantedAuthority> authorities = Stream.concat(
              grantedAuthorityStream,
              extractResourceRoles(jwt).stream()
        ).collect(Collectors.toSet());

        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}