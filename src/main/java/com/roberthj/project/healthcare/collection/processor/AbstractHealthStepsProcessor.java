package com.roberthj.project.healthcare.collection.processor;

import com.roberthj.project.healthcare.collection.aggregation.HealthStepDailyAggregationService;
import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequestEntity;
import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.collection.enums.HealthDataType;
import com.roberthj.project.healthcare.collection.model.HealthDataFormat;
import com.roberthj.project.healthcare.collection.repository.HealthStepDataJdbcRepository;
import com.roberthj.project.healthcare.collection.repository.HealthStepDataUpsertRow;
import com.roberthj.project.healthcare.member.entity.MemberEntity;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

abstract class AbstractHealthStepsProcessor implements HealthDataProcessor {

    private final HealthStepDataJdbcRepository stepDataRepository;
    private final HealthStepDailyAggregationService aggregationService;

    protected AbstractHealthStepsProcessor(HealthStepDataJdbcRepository stepDataRepository, HealthStepDailyAggregationService aggregationService) {
        this.stepDataRepository = stepDataRepository;
        this.aggregationService = aggregationService;
    }

    @Override
    public final HealthDataFormat format() {
        return new HealthDataFormat(source(), HealthDataType.STEPS);
    }

    @Override
    public final void process(HealthDataCollectionRequestEntity request) {
        // 1. Source별 입력을 UTC와 BigDecimal 기반의 걸음 데이터로 정규화
        List<HealthStepDataUpsertRow> rows = createRows(request);

        // 2. 정상 처리 순서에 따라 동일 활동 구간을 일괄 등록하거나 갱신
        stepDataRepository.upsertAll(rows);

        // 3. 이번 요청이 영향을 준 시작 일자를 기준으로 일별 집계를 다시 계산
        aggregate(request.getMember(), rows);
    }

    @Override
    public final void reprocess(HealthDataCollectionRequestEntity request) {
        // 1. 원본 요청을 동일한 규칙으로 다시 정규화
        List<HealthStepDataUpsertRow> rows = createRows(request);

        // 2. 수동 재처리보다 나중 요청으로 갱신된 활동 데이터는 유지하면서 일괄 반영
        stepDataRepository.upsertAllForManualRetry(rows);

        // 3. 실제 반영 결과를 기준으로 영향받은 일별 집계를 다시 계산
        aggregate(request.getMember(), rows);
    }

    protected abstract HealthDataSource source();

    protected abstract NormalizedStepData normalizeEntry(JsonNode entry);

    private List<HealthStepDataUpsertRow> createRows(HealthDataCollectionRequestEntity request) {
        Map<StepDataIdentity, NormalizedStepData> normalizedData = normalize(request.getPayload());
        MemberEntity member = request.getMember();
        Long requestId = request.getId();

        return normalizedData.values().stream()
            .map(data -> new HealthStepDataUpsertRow(member.getId(), requestId, source(), data.startedAt(),
                data.endedAt(), data.steps(), data.distance(), data.calories()))
            .toList();
    }

    private void aggregate(MemberEntity member, List<HealthStepDataUpsertRow> rows) {
        aggregationService.recalculate(member, source(), rows.stream().map(HealthStepDataUpsertRow::startedAt).toList());
    }

    private Map<StepDataIdentity, NormalizedStepData> normalize(JsonNode payload) {
        JsonNode entries = payload.path("data").path("entries");
        Map<StepDataIdentity, NormalizedStepData> normalizedData = new LinkedHashMap<>(entries.size());

        // 1. 각 활동 구간을 Source별 규칙으로 정규화
        for (JsonNode entry : entries) {
            NormalizedStepData data = normalizeEntry(entry);

            // 2. 한 요청 안의 동일한 from-to 구간은 마지막 값으로 정리
            normalizedData.put(new StepDataIdentity(data.startedAt(), data.endedAt()), data);
        }

        return normalizedData;
    }

    protected record NormalizedStepData(Instant startedAt, Instant endedAt, BigDecimal steps, BigDecimal distance, BigDecimal calories) {
    }

    private record StepDataIdentity(Instant startedAt, Instant endedAt) {
    }
}
