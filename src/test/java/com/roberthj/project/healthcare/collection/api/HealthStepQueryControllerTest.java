package com.roberthj.project.healthcare.collection.api;

import com.roberthj.project.healthcare.auth.security.AccessTokenIssuer;
import com.roberthj.project.healthcare.collection.response.HealthStepDailyResponse;
import com.roberthj.project.healthcare.collection.response.HealthStepMonthlyResponse;
import com.roberthj.project.healthcare.collection.service.HealthStepQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static com.roberthj.project.healthcare.collection.enums.HealthDataSource.HEALTH_KIT;
import static com.roberthj.project.healthcare.collection.enums.HealthDataSource.SAMSUNG_HEALTH;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class HealthStepQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccessTokenIssuer accessTokenIssuer;

    @MockitoBean
    private HealthStepQueryService queryService;

    @Test
    void queryDailyAggregationsWithYearMonthRequestParameter() throws Exception {
        String token = accessTokenIssuer.issue(1L, "record-key").value();
        when(queryService.getDaily(1L, "record-key", "record-key", YearMonth.of(2026, 7)))
            .thenReturn(List.of(new HealthStepDailyResponse(
                LocalDate.of(2026, 7, 1), SAMSUNG_HEALTH, 124L, new BigDecimal("1.23"), new BigDecimal("45.6"))));

        mockMvc.perform(get("/api/health-data/steps/daily")
                .header("Authorization", "Bearer " + token)
                .param("recordKey", "record-key")
                .param("yearMonth", "2026-07"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].date").value("2026-07-01"))
            .andExpect(jsonPath("$[0].source").value("SAMSUNG_HEALTH"))
            .andExpect(jsonPath("$[0].steps").value(124))
            .andExpect(jsonPath("$[0].distance").value(1.23))
            .andExpect(jsonPath("$[0].calories").value(45.6));
    }

    @Test
    void queryMonthlyAggregationsWithYearRequestParameter() throws Exception {
        String token = accessTokenIssuer.issue(1L, "record-key").value();
        when(queryService.getMonthly(1L, "record-key", "record-key", 2026))
            .thenReturn(List.of(new HealthStepMonthlyResponse(
                YearMonth.of(2026, 7), HEALTH_KIT, 11L, new BigDecimal("2.5"), BigDecimal.ZERO)));

        mockMvc.perform(get("/api/health-data/steps/monthly")
                .header("Authorization", "Bearer " + token)
                .param("recordKey", "record-key")
                .param("year", "2026"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].month").value("2026-07"))
            .andExpect(jsonPath("$[0].source").value("HEALTH_KIT"))
            .andExpect(jsonPath("$[0].steps").value(11))
            .andExpect(jsonPath("$[0].distance").value(2.5))
            .andExpect(jsonPath("$[0].calories").value(0));
    }

    @Test
    void useDefaultPeriodsWhenOptionalRequestParametersAreMissing() throws Exception {
        String token = accessTokenIssuer.issue(1L, "record-key").value();
        when(queryService.getDaily(1L, "record-key", "record-key", null)).thenReturn(List.of());
        when(queryService.getMonthly(1L, "record-key", "record-key", null)).thenReturn(List.of());

        mockMvc.perform(get("/api/health-data/steps/daily")
                .header("Authorization", "Bearer " + token)
                .param("recordKey", "record-key"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/health-data/steps/monthly")
                .header("Authorization", "Bearer " + token)
                .param("recordKey", "record-key"))
            .andExpect(status().isOk());

        verify(queryService).getDaily(1L, "record-key", "record-key", null);
        verify(queryService).getMonthly(1L, "record-key", "record-key", null);
    }
}
