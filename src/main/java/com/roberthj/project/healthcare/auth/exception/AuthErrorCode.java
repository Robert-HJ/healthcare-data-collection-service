package com.roberthj.project.healthcare.auth.exception;

import com.roberthj.project.healthcare.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public enum AuthErrorCode implements ErrorCode {

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH001", "이메일 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH002", "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH003", "접근 권한이 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "AUTH004", "이미 사용 중인 이메일입니다."),
    ;


    private final HttpStatusCode httpStatusCode;
    private final String errorCode;
    private final String errorMessage;

    AuthErrorCode(HttpStatusCode httpStatusCode, String errorCode, String errorMessage) {
        this.httpStatusCode = httpStatusCode;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    @Override
    public HttpStatusCode getHttpStatusCode() {
        return httpStatusCode;
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }
}
