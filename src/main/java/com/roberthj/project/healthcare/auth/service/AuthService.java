package com.roberthj.project.healthcare.auth.service;

import com.roberthj.project.healthcare.auth.request.SignUpRequest;
import com.roberthj.project.healthcare.auth.response.SignUpResponse;
import com.roberthj.project.healthcare.member.domain.entity.Member;
import com.roberthj.project.healthcare.member.domain.repository.MemberRepository;
import com.roberthj.project.healthcare.member.exception.DuplicateEmailException;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {

        // 1. 이메일 중복 체크
        if (memberRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }

        // 2. 회원 가입
        Member member = Member.create(
                request.name(),
                request.nickname(),
                request.email(),
                passwordEncoder.encode(request.password()),
                UUID.randomUUID().toString()
        );

        // 3. 동시 요청시 500에러가 아닌 기존과 동일한 400에러 응답을 위한 Catch.
        try {
            return SignUpResponse.from(memberRepository.save(member));
        } catch (DataIntegrityViolationException exception) {
            if (exception.getCause() instanceof ConstraintViolationException constraintViolationException) {
                String constraintName = constraintViolationException.getConstraintName();
                if (EMAIL_UNIQUE_CONSTRAINT.equals(constraintName)
                        || constraintName != null && constraintName.endsWith("." + EMAIL_UNIQUE_CONSTRAINT)) {
                    throw new DuplicateEmailException(exception);
                }
            }

            throw exception;
        }
    }
}
