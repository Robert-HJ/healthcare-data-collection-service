package com.roberthj.project.healthcare.auth.service;

import com.roberthj.project.healthcare.auth.request.SignUpRequest;
import com.roberthj.project.healthcare.member.domain.entity.Member;
import com.roberthj.project.healthcare.member.domain.repository.MemberRepository;
import com.roberthj.project.healthcare.member.exception.DuplicateEmailException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void convertEmailUniqueConstraintViolationToDuplicateEmailException() {
        SignUpRequest request = signUpRequest();
        DataIntegrityViolationException exception = integrityViolation("member.uk_member_email");

        when(memberRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(memberRepository.save(any(Member.class))).thenThrow(exception);

        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasCause(exception);
    }

    @Test
    void propagateOtherDataIntegrityViolation() {
        SignUpRequest request = signUpRequest();
        DataIntegrityViolationException exception = integrityViolation("uk_member_record_key");

        when(memberRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(memberRepository.save(any(Member.class))).thenThrow(exception);

        assertThatThrownBy(() -> authService.signUp(request))
                .isSameAs(exception);
    }

    private SignUpRequest signUpRequest() {
        return new SignUpRequest(
                "홍길동",
                "길동",
                "member@example.com",
                "Password!123"
        );
    }

    private DataIntegrityViolationException integrityViolation(String constraintName) {
        ConstraintViolationException constraintViolationException = new ConstraintViolationException(
                "Data integrity violation",
                new SQLException(),
                "INSERT",
                ConstraintViolationException.ConstraintKind.UNIQUE,
                constraintName
        );

        return new DataIntegrityViolationException("Data integrity violation", constraintViolationException);
    }
}
