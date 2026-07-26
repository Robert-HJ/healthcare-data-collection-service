package com.roberthj.project.healthcare.auth.exception;

import com.roberthj.project.healthcare.framework.exception.BaseException;

public class AuthException extends BaseException {

    public AuthException(AuthErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public AuthException(AuthErrorCode errorCode) {
        super(errorCode);
    }
}
