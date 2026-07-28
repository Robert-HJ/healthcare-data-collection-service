package com.roberthj.project.healthcare.member.service;

import com.roberthj.project.healthcare.member.exception.MemberException;
import com.roberthj.project.healthcare.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.roberthj.project.healthcare.member.exception.MemberErrorCode.MEMBER_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    void rejectMemberNotFound() {
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMember(1L))
            .isInstanceOfSatisfying(MemberException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(MEMBER_NOT_FOUND)
            );
    }
}
