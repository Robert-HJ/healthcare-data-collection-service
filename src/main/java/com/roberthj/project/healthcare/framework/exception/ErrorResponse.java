package com.roberthj.project.healthcare.framework.exception;

import java.time.Instant;

public record ErrorResponse(
        String errorCode,
        String errorMessage,
        String detailMessage,
        Instant timestamp
) {

    public static ErrorResponse of(ErrorCode errorCode, String detailMessage) {
        return new ErrorResponse(
                errorCode.getErrorCode(),
                errorCode.getErrorMessage(),
                detailMessage,
                Instant.now()
        );
    }
}
