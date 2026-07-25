package com.roberthj.project.healthcare.auth.service;

import com.roberthj.project.healthcare.auth.request.SignUpRequest;
import com.roberthj.project.healthcare.auth.response.SignUpResponse;
import com.roberthj.project.healthcare.member.domain.entity.Member;
import com.roberthj.project.healthcare.member.domain.repository.MemberRepository;
import com.roberthj.project.healthcare.member.exception.DuplicateEmailException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

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

        return SignUpResponse.from(memberRepository.save(member));
    }
}
