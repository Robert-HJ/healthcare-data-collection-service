package com.roberthj.project.healthcare.common.exception;

import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detailMessage;

    public BaseException(ErrorCode errorCode, String detailMessage, Throwable cause) {
        super(buildMessage(errorCode, detailMessage), cause);
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
    }

    public BaseException(ErrorCode errorCode, String detailMessage) {
        this(errorCode, detailMessage, null);
    }

    public BaseException(String detailMessage, Throwable cause) {
        this(BaseErrorCode.SERVER_ERROR, detailMessage, cause);
    }

    public BaseException(String detailMessage) {
        this(BaseErrorCode.SERVER_ERROR, detailMessage, null);
    }

    public BaseException(ErrorCode errorCode, Throwable cause) {
        this(errorCode, null, cause);
    }

    public BaseException(ErrorCode errorCode) {
        this(errorCode, null, null);
    }

    public BaseException(Throwable cause) {
        this(BaseErrorCode.SERVER_ERROR, null, cause);
    }

    private static String buildMessage(ErrorCode errorCode, String detailMessage) {
        if (detailMessage == null) {
            return String.format("[%s]", errorCode.getErrorMessage());
        }

        return String.format("[%s] : %s", errorCode.getErrorMessage(), detailMessage);
    }
}
