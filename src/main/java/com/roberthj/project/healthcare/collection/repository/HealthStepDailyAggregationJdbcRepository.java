package com.roberthj.project.healthcare.collection.repository;

import com.roberthj.project.healthcare.collection.aggregation.HealthStepDailyAggregationRow;
import com.roberthj.project.healthcare.collection.aggregation.HealthStepMonthlyAggregationRow;
import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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

    private static final String FIND_DAILY_SQL = """
        SELECT aggregate_date, source, steps, distance, calories
        FROM health_step_daily_aggregation
        WHERE member_id = ?
          AND timezone = ?
          AND aggregate_date >= ?
          AND aggregate_date < ?
        ORDER BY aggregate_date, source
        """;

    private static final String FIND_MONTHLY_SQL = """
        SELECT MONTH(aggregate_date) AS aggregate_month,
               source,
               SUM(steps) AS steps,
               SUM(distance) AS distance,
               SUM(calories) AS calories
        FROM health_step_daily_aggregation
        WHERE member_id = ?
          AND timezone = ?
          AND aggregate_date >= ?
          AND aggregate_date < ?
        GROUP BY MONTH(aggregate_date), source
        ORDER BY aggregate_month, source
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

    public List<HealthStepDailyAggregationRow> findDaily(Long memberId, String timezone, LocalDate from, LocalDate to) {
        return jdbcTemplate.query(FIND_DAILY_SQL, (resultSet, rowNumber) -> new HealthStepDailyAggregationRow(
            resultSet.getObject("aggregate_date", LocalDate.class),
            HealthDataSource.valueOf(resultSet.getString("source")),
            resultSet.getBigDecimal("steps"),
            resultSet.getBigDecimal("distance"),
            resultSet.getBigDecimal("calories")
        ), memberId, timezone, from, to);
    }

    public List<HealthStepMonthlyAggregationRow> findMonthly(Long memberId, String timezone, LocalDate from, LocalDate to) {
        return jdbcTemplate.query(FIND_MONTHLY_SQL, (resultSet, rowNumber) -> new HealthStepMonthlyAggregationRow(
            resultSet.getInt("aggregate_month"),
            HealthDataSource.valueOf(resultSet.getString("source")),
            resultSet.getBigDecimal("steps"),
            resultSet.getBigDecimal("distance"),
            resultSet.getBigDecimal("calories")
        ), memberId, timezone, from, to);
    }
}
