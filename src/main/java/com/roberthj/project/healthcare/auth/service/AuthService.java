package com.roberthj.project.healthcare.auth.service;

import com.roberthj.project.healthcare.auth.exception.AuthErrorCode;
import com.roberthj.project.healthcare.auth.exception.AuthException;
import com.roberthj.project.healthcare.auth.request.SignInRequest;
import com.roberthj.project.healthcare.auth.request.SignUpRequest;
import com.roberthj.project.healthcare.auth.response.SignInResponse;
import com.roberthj.project.healthcare.auth.response.SignUpResponse;
import com.roberthj.project.healthcare.auth.component.AccessTokenIssuer;
import com.roberthj.project.healthcare.framework.security.CustomUserDetails;
import com.roberthj.project.healthcare.member.repository.MemberRepository;
import com.roberthj.project.healthcare.member.entity.MemberEntity;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String EMAIL_UNIQUE_CONSTRAINT = "uk_member_email";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final AccessTokenIssuer accessTokenIssuer;

    /**
     * 회원 가입
     *
     * @param request 회원 가입 Request 객체
     * @return SignUpResponse
     */
    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {

        // 1. 이메일 중복 체크
        if (memberRepository.existsByEmail(request.email())) {
            throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 2. 가입 대상 객체 생성
        MemberEntity entity = MemberEntity.create(
                request.name(),
                request.nickname(),
                request.email(),
                passwordEncoder.encode(request.password()),
                UUID.randomUUID().toString()
        );

        // 3. 회원 가입 - 동시 호출로 500 에러 발생시 응답 일관성을 위한 Catch
        try {
            return SignUpResponse.from(memberRepository.save(entity));
        } catch (DataIntegrityViolationException exception) {
            if (exception.getCause() instanceof ConstraintViolationException constraintViolationException) {
                String constraintName = constraintViolationException.getConstraintName();
                if (EMAIL_UNIQUE_CONSTRAINT.equals(constraintName)
                        || constraintName != null && constraintName.endsWith("." + EMAIL_UNIQUE_CONSTRAINT)) {
                    throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS, exception);
                }
            }

            throw exception;
        }
    }

    /**
     * 로그인
     *
     * @param request 로그인 Request 객체
     * @return SignInResponse
     */
    @Transactional(readOnly = true)
    public SignInResponse signIn(SignInRequest request) {
        try {
            // 1. Security를 통한 로그인 처리
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));

            // 2. 결과 인증 객체를 통해 JWT 토큰 발급
            CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
            AccessTokenIssuer.IssuedToken token =
                    accessTokenIssuer.issue(principal.getId(), principal.getRecordKey());

            // 3. 발급 결과 반환
            return SignInResponse.from(token);
        } catch (BadCredentialsException exception) {
            // 존재하지 않는 회원과 잘못된 비밀번호는 동일한 실패로 처리
            throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS, exception);
        }
    }
}
