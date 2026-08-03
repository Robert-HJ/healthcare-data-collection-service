package com.roberthj.project.healthcare.collection.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class HealthDataCollectionRequestClaimRepository {

    // 1. 처리 가능한 상태와 재시도 조건을 만족하는 요청을 조회
    // 2. 같은 member, source, dataType 그룹의 선행 요청이 남아 있으면 후속 요청을 제외
    // 3. 다른 인스턴스가 잠근 요청은 건너뛰고 한 건만 선점
    private static final String SELECT_NEXT_REQUEST = """
        SELECT candidate.id
        FROM health_data_collection_request candidate
        WHERE (
              candidate.status = 'PENDING'
              OR (candidate.status = 'FAILED' AND candidate.retry_count < ?)
              OR (candidate.status = 'PROCESSING' AND candidate.updated_at < DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL ? SECOND))
          )
          AND NOT EXISTS (
              SELECT 1
              FROM health_data_collection_request previous_request
              WHERE previous_request.member_id = candidate.member_id
                AND previous_request.source = candidate.source
                AND previous_request.data_type = candidate.data_type
                AND previous_request.id < candidate.id
                AND (
                    previous_request.status IN ('PENDING', 'PROCESSING')
                    OR (previous_request.status = 'FAILED' AND previous_request.retry_count < ?)
                )
          )
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """;

    private static final String UPDATE_PROCESSING = """
        UPDATE health_data_collection_request
        SET status = 'PROCESSING', updated_at = CURRENT_TIMESTAMP(6)
        WHERE id = ?
        """;

    private static final String CLAIM_FOR_MANUAL_REPROCESSING = """
        UPDATE health_data_collection_request
        SET status = 'PROCESSING', updated_at = CURRENT_TIMESTAMP(6)
        WHERE id = ?
          AND (status <> 'PROCESSING' OR updated_at < DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL ? SECOND))
        """;

    private final JdbcTemplate jdbcTemplate;

    public Optional<Long> findNextRequest(int maxRetryCount, Duration staleProcessingTimeout) {
        List<Long> requestIds = jdbcTemplate.query(SELECT_NEXT_REQUEST,
            (resultSet, rowNumber) -> resultSet.getLong("id"), maxRetryCount, staleProcessingTimeout.toSeconds(), maxRetryCount);
        return requestIds.stream().findFirst();
    }

    public void updateProcessing(Long requestId) {
        jdbcTemplate.update(UPDATE_PROCESSING, requestId);
    }

    public boolean claimForManualReprocessing(Long requestId, Duration staleProcessingTimeout) {
        return jdbcTemplate.update(CLAIM_FOR_MANUAL_REPROCESSING, requestId, staleProcessingTimeout.toSeconds()) == 1;
    }
}
