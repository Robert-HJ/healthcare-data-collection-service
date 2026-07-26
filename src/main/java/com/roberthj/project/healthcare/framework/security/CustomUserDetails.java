package com.roberthj.project.healthcare.framework.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 인증 과정에서 사용하는 인증 객체
 * 로그인 성공 후 토큰 발급에 필요한 회원 식별값과 recordKey를 함께 전달한다.
 */
public class CustomUserDetails implements UserDetails {

    @Getter
    private final Long id;
    @Getter
    private final String recordKey;

    private final String email;
    private final String password;

    public CustomUserDetails(Long id, String email, String password, String recordKey) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.recordKey = recordKey;
    }

    // 역할(Role)은 두지 않으므로 비움 처리
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
