package com.roberthj.project.healthcare.collection.entity;

import com.roberthj.project.healthcare.collection.enums.CollectionRequestStatus;
import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.collection.enums.HealthDataType;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

@Getter
@Entity
@Table(
    name = "health_data_collection_request",
    indexes = {
        @Index(
            name = "idx_health_data_collection_request_status_updated_at",
            columnList = "status, updated_at"
        ),
        @Index(
            name = "idx_health_data_collection_request_group_order",
            columnList = "member_id, source, data_type, id"
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthDataCollectionRequestEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private MemberEntity member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private HealthDataType dataType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 50)
    private HealthDataSource source;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "json")
    private JsonNode payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CollectionRequestStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(length = 1000)
    private String errorMessage;

    private HealthDataCollectionRequestEntity(MemberEntity member, HealthDataType dataType, HealthDataSource source, JsonNode payload) {
        this.member = member;
        this.dataType = dataType;
        this.source = source;
        this.payload = payload;
        this.status = CollectionRequestStatus.PENDING;
        this.retryCount = 0;
    }

    public static HealthDataCollectionRequestEntity create(MemberEntity member, HealthDataType dataType, HealthDataSource source, JsonNode payload) {
        return new HealthDataCollectionRequestEntity(member, dataType, source, payload);
    }

    public void complete() {
        this.status = CollectionRequestStatus.COMPLETED;
    }

    public void fail(String errorMessage) {
        this.status = CollectionRequestStatus.FAILED;
        this.retryCount++;
        this.errorMessage = errorMessage;
    }
}
