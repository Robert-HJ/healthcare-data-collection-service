package com.roberthj.project.healthcare.auth.api;

import com.roberthj.project.healthcare.auth.request.SignInRequest;
import com.roberthj.project.healthcare.auth.request.SignUpRequest;
import com.roberthj.project.healthcare.auth.response.MeResponse;
import com.roberthj.project.healthcare.auth.response.SignInResponse;
import com.roberthj.project.healthcare.auth.response.SignUpResponse;
import com.roberthj.project.healthcare.auth.component.AccessTokenIssuer;
import com.roberthj.project.healthcare.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.roberthj.project.healthcare.framework.response.ResponseEntityFactory.created;
import static com.roberthj.project.healthcare.framework.response.ResponseEntityFactory.ok;

@Tag(name = "Authentication")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입")
    @PostMapping("/sign-up")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        return created(authService.signUp(request));
    }

    @Operation(summary = "로그인")
    @PostMapping("/sign-in")
    public ResponseEntity<SignInResponse> signIn(@Valid @RequestBody SignInRequest request) {
        return ok(authService.signIn(request));
    }

    @Operation(summary = "내 인증 정보 조회")
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return ok(new MeResponse(
                Long.valueOf(jwt.getSubject()),
                jwt.getClaimAsString(AccessTokenIssuer.RECORD_KEY_CLAIM)
        ));
    }
}
