package com.roberthj.project.healthcare.collection.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class HealthStepDataJdbcRepository {

    private static final String INSERT_SQL = """
        INSERT INTO health_step_data (
            member_id,
            collection_request_id,
            source,
            started_at,
            ended_at,
            steps,
            distance,
            calories,
            created_at,
            updated_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)) AS incoming
        """;

    private static final String UPSERT_SQL = INSERT_SQL + """
        ON DUPLICATE KEY UPDATE
            collection_request_id = incoming.collection_request_id,
            steps = incoming.steps,
            distance = incoming.distance,
            calories = incoming.calories,
            updated_at = incoming.updated_at
        """;

    private static final String MANUAL_RETRY_UPSERT_SQL = INSERT_SQL + """
        ON DUPLICATE KEY UPDATE
            steps = IF(
                health_step_data.collection_request_id <= incoming.collection_request_id,
                incoming.steps,
                health_step_data.steps
            ),
            distance = IF(
                health_step_data.collection_request_id <= incoming.collection_request_id,
                incoming.distance,
                health_step_data.distance
            ),
            calories = IF(
                health_step_data.collection_request_id <= incoming.collection_request_id,
                incoming.calories,
                health_step_data.calories
            ),
            updated_at = IF(
                health_step_data.collection_request_id <= incoming.collection_request_id,
                incoming.updated_at,
                health_step_data.updated_at
            ),
            collection_request_id = GREATEST(
                health_step_data.collection_request_id,
                incoming.collection_request_id
            )
        """;

    private final JdbcTemplate jdbcTemplate;

    public void upsertAll(List<HealthStepDataUpsertRow> rows) {
        executeBatch(UPSERT_SQL, rows);
    }

    public void upsertAllForManualRetry(List<HealthStepDataUpsertRow> rows) {
        executeBatch(MANUAL_RETRY_UPSERT_SQL, rows);
    }

    private void executeBatch(String sql, List<HealthStepDataUpsertRow> rows) {
        if (rows.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
            sql,
            rows,
            rows.size(),
            (statement, row) -> {
                statement.setLong(1, row.memberId());
                statement.setLong(2, row.collectionRequestId());
                statement.setString(3, row.source().name());
                statement.setObject(4, LocalDateTime.ofInstant(row.startedAt(), ZoneOffset.UTC));
                statement.setObject(5, LocalDateTime.ofInstant(row.endedAt(), ZoneOffset.UTC));
                statement.setBigDecimal(6, row.steps());
                statement.setBigDecimal(7, row.distance());
                statement.setBigDecimal(8, row.calories());
            }
        );
    }
}
