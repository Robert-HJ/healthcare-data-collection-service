package com.roberthj.project.healthcare.collection.processor;

import com.roberthj.project.healthcare.collection.aggregation.HealthStepDailyAggregationService;
import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequestEntity;
import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.collection.enums.HealthDataType;
import com.roberthj.project.healthcare.collection.model.HealthDataFormat;
import com.roberthj.project.healthcare.collection.repository.HealthStepDataJdbcRepository;
import com.roberthj.project.healthcare.collection.repository.HealthStepDataUpsertRow;
import com.roberthj.project.healthcare.member.entity.MemberEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthKitStepsProcessorTest {

    private static final String FIXTURE_PATH = "/fixtures/collection/health-kit-valid.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private HealthStepDataJdbcRepository stepDataRepository;

    @Mock
    private HealthStepDailyAggregationService aggregationService;

    @Mock
    private HealthDataCollectionRequestEntity request;

    @Mock
    private MemberEntity member;

    @Captor
    private ArgumentCaptor<List<HealthStepDataUpsertRow>> rowsCaptor;

    @Captor
    private ArgumentCaptor<List<Instant>> startedAtValuesCaptor;

    @InjectMocks
    private HealthKitStepsProcessor processor;

    @Test
    void normalizeSaveAndAggregateHealthKitSteps() throws JacksonException {
        prepareRequest(loadPayload());

        processor.process(request);

        InOrder callOrder = inOrder(stepDataRepository, aggregationService);
        callOrder.verify(stepDataRepository).upsertAll(rowsCaptor.capture());
        callOrder.verify(aggregationService).recalculate(eq(member), eq(HealthDataSource.HEALTH_KIT), startedAtValuesCaptor.capture());
        List<HealthStepDataUpsertRow> rows = rowsCaptor.getValue();

        assertThat(processor.format()).isEqualTo(new HealthDataFormat(HealthDataSource.HEALTH_KIT, HealthDataType.STEPS));
        assertThat(rows).hasSize(2);
        assertThat(startedAtValuesCaptor.getValue()).containsExactly(
            Instant.parse("2024-11-14T21:20:00Z"), Instant.parse("2024-11-14T23:00:00Z"));

        HealthStepDataUpsertRow firstData = rows.get(0);
        assertThat(firstData.memberId()).isEqualTo(1L);
        assertThat(firstData.collectionRequestId()).isEqualTo(10L);
        assertThat(firstData.source()).isEqualTo(HealthDataSource.HEALTH_KIT);
        assertThat(firstData.startedAt()).isEqualTo(Instant.parse("2024-11-14T21:20:00Z"));
        assertThat(firstData.endedAt()).isEqualTo(Instant.parse("2024-11-14T21:30:00Z"));
        assertThat(firstData.steps()).isEqualByComparingTo(new BigDecimal("24"));
        assertThat(firstData.distance()).isEqualByComparingTo(new BigDecimal("0.0192"));
        assertThat(firstData.calories()).isEqualByComparingTo(BigDecimal.ZERO);

        HealthStepDataUpsertRow decimalStepsData = rows.get(1);
        assertThat(decimalStepsData.steps()).isEqualByComparingTo(new BigDecimal("287.6726411513615"));
        assertThat(decimalStepsData.distance()).isEqualByComparingTo(new BigDecimal("0.2301381129210892"));
        assertThat(decimalStepsData.calories()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void useManualRetryUpsertWhenReprocessing() throws JacksonException {
        prepareRequest(loadPayload());

        processor.reprocess(request);

        verify(stepDataRepository).upsertAllForManualRetry(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue()).hasSize(2);
    }

    private void prepareRequest(JsonNode payload) {
        when(request.getPayload()).thenReturn(payload);
        when(request.getId()).thenReturn(10L);
        when(request.getMember()).thenReturn(member);
        when(member.getId()).thenReturn(1L);
    }

    private JsonNode loadPayload() throws JacksonException {
        InputStream inputStream = Objects.requireNonNull(getClass().getResourceAsStream(FIXTURE_PATH));
        return objectMapper.readTree(inputStream);
    }
}
