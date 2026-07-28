package com.roberthj.project.healthcare.collection.exception;

import com.roberthj.project.healthcare.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public enum CollectionErrorCode implements ErrorCode {

    INVALID_PAYLOAD(HttpStatus.BAD_REQUEST, "COLLECTION001", "수집 요청 형식이 올바르지 않습니다."),
    RECORD_KEY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "COLLECTION002", "접근할 수 없는 건강 데이터입니다."),
    ;

    private final HttpStatusCode httpStatusCode;
    private final String errorCode;
    private final String errorMessage;

    CollectionErrorCode(HttpStatusCode httpStatusCode, String errorCode, String errorMessage) {
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
