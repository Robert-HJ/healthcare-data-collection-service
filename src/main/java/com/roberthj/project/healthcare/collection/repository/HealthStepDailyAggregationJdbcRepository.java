package com.roberthj.project.healthcare.collection.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class HealthStepDailyAggregationJdbcRepository {

    private static final String UPSERT_SQL = """
        INSERT INTO health_step_daily_aggregation (
            member_id,
            timezone,
            source,
            aggregate_date,
            steps,
            distance,
            calories,
            created_at,
            updated_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)) AS incoming
        ON DUPLICATE KEY UPDATE
            steps = incoming.steps,
            distance = incoming.distance,
            calories = incoming.calories,
            updated_at = incoming.updated_at
        """;

    private final JdbcTemplate jdbcTemplate;

    public void upsertAll(List<HealthStepDailyAggregationUpsertRow> rows) {
        if (rows.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
            UPSERT_SQL,
            rows,
            rows.size(),
            (statement, row) -> {
                statement.setLong(1, row.memberId());
                statement.setString(2, row.timezone());
                statement.setString(3, row.source().name());
                statement.setObject(4, row.aggregateDate());
                statement.setBigDecimal(5, row.steps());
                statement.setBigDecimal(6, row.distance());
                statement.setBigDecimal(7, row.calories());
            }
        );
    }
}
