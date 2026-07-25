package com.roberthj.project.healthcare.member.exception;

import com.roberthj.project.healthcare.framework.exception.BaseException;

public class DuplicateEmailException extends BaseException {

    public DuplicateEmailException() {
        super(MemberErrorCode.EMAIL_ALREADY_EXISTS);
    }
}
