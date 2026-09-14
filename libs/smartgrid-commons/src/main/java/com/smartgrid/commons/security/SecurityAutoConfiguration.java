package com.smartgrid.commons.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.jwt.Jwt;

@AutoConfiguration
@ConditionalOnClass(Jwt.class)
@EnableMethodSecurity
public class SecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RoleClaimJwtAuthenticationConverter roleClaimJwtAuthenticationConverter() {
        return new RoleClaimJwtAuthenticationConverter();
    }
}
