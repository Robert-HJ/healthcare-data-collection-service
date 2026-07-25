package com.roberthj.project.healthcare.auth.security;

import com.roberthj.project.healthcare.framework.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Access Token 발급기.
 */
@Component
@RequiredArgsConstructor
public class AccessTokenIssuer {

    public static final String RECORD_KEY_CLAIM = "recordKey";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public IssuedToken issue(Long memberId, String recordKey) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(jwtProperties.accessTokenValidity());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(memberId))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(RECORD_KEY_CLAIM, recordKey) // JWT 토큰에서 현재 Record Key만 체크할 예정이므로 Record Key만 추가함.
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String tokenValue = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new IssuedToken(tokenValue, jwtProperties.accessTokenValidity().getSeconds());
    }

    public record IssuedToken(String value, long expiresInSeconds) {
    }
}
