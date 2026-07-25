package com.roberthj.project.healthcare.framework.exception;

import org.springframework.http.HttpStatusCode;

public interface ErrorCode {

    HttpStatusCode getHttpStatusCode();

    String getErrorCode();

    String getErrorMessage();
}
