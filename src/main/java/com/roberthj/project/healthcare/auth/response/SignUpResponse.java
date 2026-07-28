package com.roberthj.project.healthcare.auth.response;

import com.roberthj.project.healthcare.member.entity.MemberEntity;

import java.time.Instant;

public record SignUpResponse(
        Long id,
        String name,
        String nickname,
        String email,
        String recordKey,
        Instant createdAt
) {

    public static SignUpResponse from(MemberEntity member) {
        return new SignUpResponse(
                member.getId(),
                member.getName(),
                member.getNickname(),
                member.getEmail(),
                member.getRecordKey(),
                member.getCreatedAt()
        );
    }
}
