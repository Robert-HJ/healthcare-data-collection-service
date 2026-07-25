package com.roberthj.project.healthcare.framework.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public enum BaseErrorCode implements ErrorCode {

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON001", "잘못된 요청"),
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON002", "서버 에러"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON003", "존재하지 않는 리소스"),
    CONFLICT(HttpStatus.CONFLICT, "COMMON004", "요청 상태 충돌");

    private final HttpStatusCode httpStatusCode;
    private final String errorCode;
    private final String errorMessage;

    BaseErrorCode(HttpStatusCode httpStatusCode, String errorCode, String errorMessage) {
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
