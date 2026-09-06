package com.smartgrid.authservice.dto;

public record TokenPairResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds
) {
}
