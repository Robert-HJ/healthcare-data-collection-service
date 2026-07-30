package com.roberthj.project.healthcare.collection.entity;

import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.common.entity.BaseEntity;
import com.roberthj.project.healthcare.member.entity.MemberEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Entity
@Table(
    name = "health_step_data",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_health_step_data_identity",
            columnNames = {"member_id", "source", "started_at", "ended_at"}
        )
    },
    indexes = {
        @Index(
            name = "idx_health_step_data_collection_request_id",
            columnList = "collection_request_id"
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthStepDataEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private MemberEntity member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collection_request_id", nullable = false)
    private HealthDataCollectionRequestEntity collectionRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 50)
    private HealthDataSource source;

    @Column(nullable = false, updatable = false)
    private Instant startedAt;

    @Column(nullable = false, updatable = false)
    private Instant endedAt;

    @Column(nullable = false, precision = 30, scale = 20)
    private BigDecimal steps;

    @Column(nullable = false, precision = 30, scale = 20)
    private BigDecimal distance;

    @Column(nullable = false, precision = 30, scale = 20)
    private BigDecimal calories;

    private HealthStepDataEntity(
        MemberEntity member,
        HealthDataCollectionRequestEntity collectionRequest,
        HealthDataSource source,
        Instant startedAt,
        Instant endedAt,
        BigDecimal steps,
        BigDecimal distance,
        BigDecimal calories
    ) {
        this.member = member;
        this.collectionRequest = collectionRequest;
        this.source = source;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.steps = steps;
        this.distance = distance;
        this.calories = calories;
    }

    public static HealthStepDataEntity create(
        MemberEntity member,
        HealthDataCollectionRequestEntity collectionRequest,
        HealthDataSource source,
        Instant startedAt,
        Instant endedAt,
        BigDecimal steps,
        BigDecimal distance,
        BigDecimal calories
    ) {
        return new HealthStepDataEntity(
            member,
            collectionRequest,
            source,
            startedAt,
            endedAt,
            steps,
            distance,
            calories
        );
    }

    public void update(
        HealthDataCollectionRequestEntity collectionRequest,
        BigDecimal steps,
        BigDecimal distance,
        BigDecimal calories
    ) {
        this.collectionRequest = collectionRequest;
        this.steps = steps;
        this.distance = distance;
        this.calories = calories;
    }
}
