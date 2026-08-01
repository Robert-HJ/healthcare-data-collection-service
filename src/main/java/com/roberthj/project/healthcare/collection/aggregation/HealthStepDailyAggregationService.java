package com.roberthj.project.healthcare.collection.aggregation;

import com.roberthj.project.healthcare.collection.config.HealthStepAggregationProperties;
import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.collection.repository.HealthStepDailyAggregationJdbcRepository;
import com.roberthj.project.healthcare.collection.repository.HealthStepDailyAggregationUpsertRow;
import com.roberthj.project.healthcare.collection.repository.HealthStepDataRepository;
import com.roberthj.project.healthcare.member.entity.MemberEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HealthStepDailyAggregationService {

    private final HealthStepAggregationProperties aggregationProperties;
    private final HealthStepDataRepository stepDataRepository;
    private final HealthStepDailyAggregationJdbcRepository dailyAggregationRepository;

    @Transactional
    public void recalculate(MemberEntity member, HealthDataSource source, List<Instant> affectedStartedAtValues) {
        List<HealthStepDailyAggregationUpsertRow> upsertRows = new ArrayList<>();

        for (ZoneId timeZone : aggregationProperties.timeZones()) {
            upsertRows.addAll(recalculateForTimeZone(member.getId(), source, affectedStartedAtValues, timeZone));
        }

        dailyAggregationRepository.upsertAll(upsertRows);
    }

    private List<HealthStepDailyAggregationUpsertRow> recalculateForTimeZone(
        Long memberId, HealthDataSource source, List<Instant> affectedStartedAtValues, ZoneId timeZone) {
        List<LocalDate> affectedDates = resolveAffectedDates(affectedStartedAtValues, timeZone);
        if (affectedDates.isEmpty()) {
            return List.of();
        }

        Instant rangeStart = affectedDates.get(0).atStartOfDay(timeZone).toInstant();
        Instant rangeEnd = affectedDates.get(affectedDates.size() - 1).plusDays(1).atStartOfDay(timeZone).toInstant();
        List<HealthStepAggregationSourceRow> sourceRows = stepDataRepository.findByStartedAtRange(memberId, source, rangeStart, rangeEnd);

        Map<LocalDate, HealthStepAggregationTotals> totalsByDate = new LinkedHashMap<>();
        for (LocalDate affectedDate : affectedDates) {
            totalsByDate.put(affectedDate, HealthStepAggregationTotals.ZERO);
        }

        for (HealthStepAggregationSourceRow sourceRow : sourceRows) {
            LocalDate aggregateDate = sourceRow.startedAt().atZone(timeZone).toLocalDate();
            totalsByDate.computeIfPresent(aggregateDate, (date, totals) -> totals.add(sourceRow));
        }

        return totalsByDate.entrySet().stream().map(
            entry -> toUpsertRow(memberId, source, timeZone, entry.getKey(), entry.getValue())).toList();
    }

    private List<LocalDate> resolveAffectedDates(List<Instant> affectedStartedAtValues, ZoneId timeZone) {
        return affectedStartedAtValues.stream().map(startedAt -> startedAt.atZone(timeZone).toLocalDate()).distinct().sorted().toList();
    }

    private HealthStepDailyAggregationUpsertRow toUpsertRow(Long memberId, HealthDataSource source, ZoneId timeZone,
                                                            LocalDate aggregateDate, HealthStepAggregationTotals totals) {
        return new HealthStepDailyAggregationUpsertRow(memberId, timeZone.getId(), source, aggregateDate,
            totals.steps(), totals.distance(), totals.calories());
    }
}
