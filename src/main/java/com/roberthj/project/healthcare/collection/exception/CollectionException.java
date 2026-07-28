package com.roberthj.project.healthcare.collection.exception;

import com.roberthj.project.healthcare.common.exception.BaseException;

public class CollectionException extends BaseException {

    public CollectionException(CollectionErrorCode errorCode, String detailMessage, Throwable cause) {
        super(errorCode, detailMessage, cause);
    }

    public CollectionException(CollectionErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }

    public CollectionException(CollectionErrorCode errorCode) {
        super(errorCode);
    }
}
