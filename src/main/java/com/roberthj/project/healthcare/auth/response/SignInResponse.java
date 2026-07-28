package com.roberthj.project.healthcare.auth.response;

import com.roberthj.project.healthcare.auth.security.AccessTokenIssuer;

public record SignInResponse(
        String tokenType,
        String accessToken,
        long expiresIn
) {

    public static SignInResponse from(AccessTokenIssuer.IssuedToken token) {
        return new SignInResponse("Bearer", token.value(), token.expiresInSeconds());
    }
}
