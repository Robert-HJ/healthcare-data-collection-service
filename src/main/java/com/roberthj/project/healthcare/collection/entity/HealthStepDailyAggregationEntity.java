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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Entity
@Table(
    name = "health_step_daily_aggregation",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_health_step_daily_aggregation",
            columnNames = {"member_id", "timezone", "source", "aggregate_date"}
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthStepDailyAggregationEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private MemberEntity member;

    @Column(nullable = false, updatable = false, length = 50)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 50)
    private HealthDataSource source;

    @Column(nullable = false, updatable = false)
    private LocalDate aggregateDate;

    @Column(nullable = false, precision = 30, scale = 20)
    private BigDecimal steps;

    @Column(nullable = false, precision = 30, scale = 20)
    private BigDecimal distance;

    @Column(nullable = false, precision = 30, scale = 20)
    private BigDecimal calories;

    private HealthStepDailyAggregationEntity(
        MemberEntity member,
        String timezone,
        HealthDataSource source,
        LocalDate aggregateDate,
        BigDecimal steps,
        BigDecimal distance,
        BigDecimal calories
    ) {
        this.member = member;
        this.timezone = timezone;
        this.source = source;
        this.aggregateDate = aggregateDate;
        this.steps = steps;
        this.distance = distance;
        this.calories = calories;
    }

    public static HealthStepDailyAggregationEntity create(
        MemberEntity member,
        String timezone,
        HealthDataSource source,
        LocalDate aggregateDate,
        BigDecimal steps,
        BigDecimal distance,
        BigDecimal calories
    ) {
        return new HealthStepDailyAggregationEntity(
            member,
            timezone,
            source,
            aggregateDate,
            steps,
            distance,
            calories
        );
    }

}
