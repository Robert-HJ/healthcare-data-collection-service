package com.roberthj.project.healthcare.member.exception;

import com.roberthj.project.healthcare.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public enum MemberErrorCode implements ErrorCode {

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER001", "존재하지 않는 회원입니다."),
    ;

    private final HttpStatusCode httpStatusCode;
    private final String errorCode;
    private final String errorMessage;

    MemberErrorCode(HttpStatusCode httpStatusCode, String errorCode, String errorMessage) {
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
