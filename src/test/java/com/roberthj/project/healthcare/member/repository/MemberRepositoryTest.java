package com.roberthj.project.healthcare.member.repository;

import com.roberthj.project.healthcare.framework.config.JpaConfig;
import com.roberthj.project.healthcare.member.entity.MemberEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
@Import(JpaConfig.class)
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void saveMember() {
        String recordKey = UUID.randomUUID().toString();
        MemberEntity member = MemberEntity.create(
            "홍길동",
            "길동",
            "member-" + recordKey + "@example.com",
            "encoded-password",
            recordKey
        );

        MemberEntity savedMember = memberRepository.saveAndFlush(member);

        assertThat(savedMember.getId()).isNotNull();
        assertThat(savedMember.getRecordKey()).isEqualTo(recordKey);
        assertThat(savedMember.getCreatedAt()).isNotNull();
        assertThat(savedMember.getUpdatedAt()).isNotNull();
    }
}
