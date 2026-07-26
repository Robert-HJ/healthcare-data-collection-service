package com.roberthj.project.healthcare.auth.application;

import com.roberthj.project.healthcare.auth.component.AccessTokenIssuer;
import com.roberthj.project.healthcare.auth.exception.AuthErrorCode;
import com.roberthj.project.healthcare.auth.exception.AuthException;
import com.roberthj.project.healthcare.auth.request.SignInRequest;
import com.roberthj.project.healthcare.auth.request.SignUpRequest;
import com.roberthj.project.healthcare.auth.response.SignInResponse;
import com.roberthj.project.healthcare.auth.response.SignUpResponse;
import com.roberthj.project.healthcare.framework.security.CustomUserDetails;
import com.roberthj.project.healthcare.framework.security.CustomUserDetailsService;
import com.roberthj.project.healthcare.member.domain.entity.Member;
import com.roberthj.project.healthcare.member.domain.repository.MemberRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AccessTokenIssuer accessTokenIssuer;

    @InjectMocks
    private AuthService authService;

    @Test
    void signUpMember() {
        SignUpRequest request = signUpRequest();

        when(memberRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(memberRepository.save(any(Member.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SignUpResponse response = authService.signUp(request);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        Member savedMember = memberCaptor.getValue();

        assertThat(savedMember.getName()).isEqualTo(request.name());
        assertThat(savedMember.getNickname()).isEqualTo(request.nickname());
        assertThat(savedMember.getEmail()).isEqualTo(request.email());
        assertThat(savedMember.getPassword()).isEqualTo("encoded-password");
        assertThatUuid(savedMember.getRecordKey());
        assertThat(response.recordKey()).isEqualTo(savedMember.getRecordKey());
    }

    @Test
    void rejectExistingEmailBeforeSave() {
        SignUpRequest request = signUpRequest();

        when(memberRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOfSatisfying(AuthException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(AuthErrorCode.EMAIL_ALREADY_EXISTS));

        verify(passwordEncoder, never()).encode(any());
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void convertEmailUniqueConstraintViolationToEmailAlreadyExistsException() {
        SignUpRequest request = signUpRequest();
        DataIntegrityViolationException exception = integrityViolation("member.uk_member_email");

        when(memberRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(memberRepository.save(any(Member.class))).thenThrow(exception);

        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOfSatisfying(AuthException.class, authException -> {
                    assertThat(authException.getErrorCode())
                            .isEqualTo(AuthErrorCode.EMAIL_ALREADY_EXISTS);
                    assertThat(authException).hasCause(exception);
                });
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

    @Test
    void signInAndIssueAccessToken() {
        SignInRequest request = signInRequest();
        CustomUserDetails principal = new CustomUserDetails(
                1L,
                request.email(),
                "encoded-password",
                "record-key"
        );
        Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities()
        );
        AccessTokenIssuer.IssuedToken issuedToken =
                new AccessTokenIssuer.IssuedToken("access-token", 3600L);

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authenticated);
        when(accessTokenIssuer.issue(principal.getId(), principal.getRecordKey()))
                .thenReturn(issuedToken);

        SignInResponse response = authService.signIn(request);

        ArgumentCaptor<Authentication> authenticationCaptor =
                ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(authenticationCaptor.capture());
        Authentication requestedAuthentication = authenticationCaptor.getValue();

        assertThat(requestedAuthentication.getPrincipal()).isEqualTo(request.email());
        assertThat(requestedAuthentication.getCredentials()).isEqualTo(request.password());
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isEqualTo(issuedToken.value());
        assertThat(response.expiresIn()).isEqualTo(issuedToken.expiresInSeconds());
    }

    @Test
    void convertBadCredentialsToInvalidCredentialsException() {
        SignInRequest request = signInRequest();
        BadCredentialsException exception = new BadCredentialsException("Bad credentials");

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(exception);

        assertThatThrownBy(() -> authService.signIn(request))
                .isInstanceOfSatisfying(AuthException.class, authException -> {
                    assertThat(authException.getErrorCode())
                            .isEqualTo(AuthErrorCode.INVALID_CREDENTIALS);
                    assertThat(authException).hasCause(exception);
                });
    }

    @Test
    void convertUnknownMemberToBadCredentialsException() {
        SignInRequest request = signInRequest();
        CustomUserDetailsService userDetailsService =
                new CustomUserDetailsService(memberRepository);
        DaoAuthenticationProvider authenticationProvider =
                new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(new BCryptPasswordEncoder());
        AuthenticationManager actualAuthenticationManager =
                new ProviderManager(authenticationProvider);
        AuthService service = new AuthService(
                memberRepository,
                passwordEncoder,
                actualAuthenticationManager,
                accessTokenIssuer
        );

        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.signIn(request))
                .isInstanceOfSatisfying(AuthException.class, authException -> {
                    assertThat(authException.getErrorCode())
                            .isEqualTo(AuthErrorCode.INVALID_CREDENTIALS);
                    assertThat(authException.getCause())
                            .isInstanceOf(BadCredentialsException.class);
                });
    }

    @Test
    void propagateInternalAuthenticationFailure() {
        SignInRequest request = signInRequest();
        InternalAuthenticationServiceException exception =
                new InternalAuthenticationServiceException("Authentication service failure");

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(exception);

        assertThatThrownBy(() -> authService.signIn(request))
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

    private SignInRequest signInRequest() {
        return new SignInRequest("member@example.com", "Password!123");
    }

    private void assertThatUuid(String value) {
        assertThat(UUID.fromString(value)).isNotNull();
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
