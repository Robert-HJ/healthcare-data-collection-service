package com.roberthj.project.healthcare.member.domain.entity;

import com.roberthj.project.healthcare.framework.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "member",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_member_record_key", columnNames = "record_key")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, updatable = false, length = 36)
    private String recordKey;

    private Member(String name, String nickname, String email, String password, String recordKey) {
        this.name = name;
        this.nickname = nickname;
        this.email = email;
        this.password = password;
        this.recordKey = recordKey;
    }

    public static Member create(String name, String nickname, String email, String password, String recordKey) {
        return new Member(name, nickname, email, password, recordKey);
    }
}
