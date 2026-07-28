package com.roberthj.project.healthcare.member.service;

import com.roberthj.project.healthcare.member.entity.Member;
import com.roberthj.project.healthcare.member.exception.MemberException;
import com.roberthj.project.healthcare.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.roberthj.project.healthcare.member.exception.MemberErrorCode.MEMBER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberException(MEMBER_NOT_FOUND));
    }
}
