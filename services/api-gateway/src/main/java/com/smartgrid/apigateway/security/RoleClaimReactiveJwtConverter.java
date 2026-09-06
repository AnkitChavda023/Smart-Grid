package com.smartgrid.apigateway.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

/**
 * Reactive counterpart of smartgrid-commons' RoleClaimJwtAuthenticationConverter: the gateway runs on
 * WebFlux/Netty and cannot depend on smartgrid-commons, whose WebAutoConfiguration hard-references
 * servlet-only Spring MVC classes and would fail to load on a reactive-only classpath.
 */
public class RoleClaimReactiveJwtConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    private static final String ROLE_CLAIM = "role";
    private static final String AUTHORITY_PREFIX = "ROLE_";

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        String role = jwt.getClaimAsString(ROLE_CLAIM);
        Collection<GrantedAuthority> authorities = role == null
                ? List.of()
                : List.of(new SimpleGrantedAuthority(AUTHORITY_PREFIX + role));
        return Mono.just(new JwtAuthenticationToken(jwt, authorities, jwt.getSubject()));
    }
}
