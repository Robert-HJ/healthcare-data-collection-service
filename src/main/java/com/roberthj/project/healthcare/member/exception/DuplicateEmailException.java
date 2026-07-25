package com.roberthj.project.healthcare.member.exception;

import com.roberthj.project.healthcare.framework.exception.BaseException;

public class DuplicateEmailException extends BaseException {

    public DuplicateEmailException() {
        super(MemberErrorCode.EMAIL_ALREADY_EXISTS);
    }

    public DuplicateEmailException(Throwable cause) {
        super(MemberErrorCode.EMAIL_ALREADY_EXISTS, cause);
    }
}
