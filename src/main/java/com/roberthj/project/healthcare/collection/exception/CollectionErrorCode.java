package com.roberthj.project.healthcare.collection.exception;

import com.roberthj.project.healthcare.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public enum CollectionErrorCode implements ErrorCode {

    INVALID_PAYLOAD(HttpStatus.BAD_REQUEST, "COLLECTION001", "수집 요청 형식이 올바르지 않습니다."),
    RECORD_KEY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "COLLECTION002", "접근할 수 없는 건강 데이터입니다."),
    PROCESSOR_NOT_FOUND(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "COLLECTION003",
        "건강 데이터 처리기를 찾을 수 없습니다."
    ),
    PROCESSOR_ALREADY_REGISTERED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "COLLECTION004",
        "건강 데이터 처리기가 중복 등록되었습니다."
    ),
    COLLECTION_REQUEST_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "COLLECTION005", "건강 데이터 수집 요청을 찾을 수 없습니다."),
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
