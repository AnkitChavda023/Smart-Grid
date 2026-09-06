package com.smartgrid.authservice.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.smartgrid.authservice.domain.User;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class TokenService {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    private static final String ISSUER = "smartgrid-auth-service";

    private final RSAKey rsaJwk;

    public TokenService(RSAKey rsaJwk) {
        this.rsaJwk = rsaJwk;
    }

    public Duration accessTokenTtl() {
        return ACCESS_TOKEN_TTL;
    }

    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(ISSUER)
                    .subject(user.getUsername())
                    .claim("role", user.getRole().name())
                    .claim("userId", user.getId().toString())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plus(ACCESS_TOKEN_TTL)))
                    .build();

            SignedJWT signedJwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJwk.getKeyID()).build(),
                    claims
            );
            signedJwt.sign(new RSASSASigner(rsaJwk.toPrivateKey()));
            return signedJwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign access token", e);
        }
    }
}
