package com.roberthj.project.healthcare.auth.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.roberthj.project.healthcare.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessTokenIssuerTest {

    private static final String SECRET = "test-hmac-secret-please-change-this-value-0123456789";
    private static final String OTHER_SECRET = "another-hmac-secret-different-from-the-first-9876543210";

    private static SecretKey secretKey(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static JwtEncoder encoder(String secret) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(secret)));
    }

    private static JwtDecoder decoder(String secret) {
        return NimbusJwtDecoder.withSecretKey(secretKey(secret))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private static AccessTokenIssuer issuer(String secret, Duration validity) {
        JwtProperties properties = new JwtProperties(secret, validity);
        return new AccessTokenIssuer(encoder(secret), properties);
    }

    @Test
    void issueValidTokenWithMemberSubjectAndRecordKeyClaim() {
        AccessTokenIssuer issuer = issuer(SECRET, Duration.ofHours(1));

        AccessTokenIssuer.IssuedToken token = issuer.issue(42L, "record-key-value");

        Jwt decoded = decoder(SECRET).decode(token.value());
        assertThat(decoded.getSubject()).isEqualTo("42");
        assertThat(decoded.getClaimAsString(AccessTokenIssuer.RECORD_KEY_CLAIM)).isEqualTo("record-key-value");
        assertThat(decoded.getId()).isNotBlank();
        assertThat(decoded.getExpiresAt()).isAfter(decoded.getIssuedAt());
        assertThat(token.expiresInSeconds()).isEqualTo(Duration.ofHours(1).getSeconds());
    }

    @Test
    void rejectExpiredToken() {
        // iat·exp를 모두 과거로(단 exp가 iat보다 뒤) 두어 이미 만료된 토큰을 만든다.
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("1")
                .issuedAt(now.minus(Duration.ofHours(2)))
                .expiresAt(now.minus(Duration.ofHours(1)))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String expiredToken = encoder(SECRET)
                .encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();

        assertThatThrownBy(() -> decoder(SECRET).decode(expiredToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectTokenSignedWithDifferentKey() {
        AccessTokenIssuer issuer = issuer(SECRET, Duration.ofHours(1));

        AccessTokenIssuer.IssuedToken token = issuer.issue(1L, "record-key-value");

        assertThatThrownBy(() -> decoder(OTHER_SECRET).decode(token.value()))
                .isInstanceOf(JwtException.class);
    }
}
