package com.roberthj.project.healthcare.member.exception;

import com.roberthj.project.healthcare.framework.exception.BaseException;

public class MemberException extends BaseException {

    public MemberException(MemberErrorCode errorCode) {
        super(errorCode);
    }
}
