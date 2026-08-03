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

        // 1. 지원하는 타임존마다 영향받은 일자의 집계 결과를 생성
        for (ZoneId timeZone : aggregationProperties.timeZones()) {
            upsertRows.addAll(recalculateForTimeZone(member.getId(), source, affectedStartedAtValues, timeZone));
        }

        // 2. 타임존별 집계 결과를 한 번에 등록하거나 갱신
        dailyAggregationRepository.upsertAll(upsertRows);
    }

    private List<HealthStepDailyAggregationUpsertRow> recalculateForTimeZone(
        Long memberId, HealthDataSource source, List<Instant> affectedStartedAtValues, ZoneId timeZone) {
        // 1. 변경된 활동 시작 시각을 현재 타임존의 집계 일자로 변환
        List<LocalDate> affectedDates = resolveAffectedDates(affectedStartedAtValues, timeZone);
        if (affectedDates.isEmpty()) {
            return List.of();
        }

        // 2. 영향받은 첫날부터 마지막 날까지의 활동 데이터를 다시 조회
        Instant rangeStart = affectedDates.get(0).atStartOfDay(timeZone).toInstant();
        Instant rangeEnd = affectedDates.get(affectedDates.size() - 1).plusDays(1).atStartOfDay(timeZone).toInstant();
        List<HealthStepAggregationSourceRow> sourceRows = stepDataRepository.findByStartedAtRange(memberId, source, rangeStart, rangeEnd);

        // 3. 집계 대상 날짜에 합산할 활동이 없어도 0으로 갱신할 수 있도록 먼저 초기화
        Map<LocalDate, HealthStepAggregationTotals> totalsByDate = new LinkedHashMap<>();
        for (LocalDate affectedDate : affectedDates) {
            totalsByDate.put(affectedDate, HealthStepAggregationTotals.ZERO);
        }

        // 4. 저장된 활동 데이터를 집계 일자별로 전량 합산
        for (HealthStepAggregationSourceRow sourceRow : sourceRows) {
            LocalDate aggregateDate = sourceRow.startedAt().atZone(timeZone).toLocalDate();
            totalsByDate.computeIfPresent(aggregateDate, (date, totals) -> totals.add(sourceRow));
        }

        // 5. 계산 결과를 일별 집계 upsert 형태로 변환
        return totalsByDate.entrySet().stream().map(
            entry -> toUpsertRow(memberId, source, timeZone, entry.getKey(), entry.getValue())).toList();
    }

    private List<LocalDate> resolveAffectedDates(List<Instant> affectedStartedAtValues, ZoneId timeZone) {
        return affectedStartedAtValues.stream().map(startedAt -> startedAt.atZone(timeZone).toLocalDate()).distinct().sorted().toList();
    }

    private HealthStepDailyAggregationUpsertRow toUpsertRow(Long memberId, HealthDataSource source, ZoneId timeZone,
                                                            LocalDate aggregateDate, HealthStepAggregationTotals totals) {
        return new HealthStepDailyAggregationUpsertRow(memberId, timeZone.getId(), source, aggregateDate, totals.steps(), totals.distance(), totals.calories());
    }
}
