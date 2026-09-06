package com.smartgrid.authservice.web;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class JwksController {

    private final RSAKey rsaJwk;

    public JwksController(RSAKey rsaJwk) {
        this.rsaJwk = rsaJwk;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return new JWKSet(rsaJwk.toPublicJWK()).toJSONObject();
    }
}
