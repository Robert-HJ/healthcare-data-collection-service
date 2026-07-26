package com.roberthj.project.healthcare.framework.security;

import com.roberthj.project.healthcare.auth.exception.AuthErrorCode;
import com.roberthj.project.healthcare.framework.exception.ErrorResponse;
import com.roberthj.project.healthcare.framework.utils.CommonUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 인증되지 않은 요청(토큰 없음/무효/만료)을 공통 에러 형식으로 응답 - 401 UnAuthorized
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        AuthErrorCode errorCode = AuthErrorCode.UNAUTHORIZED;
        response.setStatus(errorCode.getHttpStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        CommonUtils.COMMON_MAPPER.writeValue(response.getWriter(), ErrorResponse.of(errorCode, null));
    }
}
