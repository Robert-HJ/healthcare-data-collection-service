package com.roberthj.project.healthcare.collection.api;

import com.roberthj.project.healthcare.auth.security.AccessTokenIssuer;
import com.roberthj.project.healthcare.collection.response.HealthStepDailyResponse;
import com.roberthj.project.healthcare.collection.response.HealthStepMonthlyResponse;
import com.roberthj.project.healthcare.collection.service.HealthStepQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

import static com.roberthj.project.healthcare.common.response.ResponseEntityFactory.ok;

@Tag(name = "Health Step Query")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/health-data/steps")
public class HealthStepQueryController {

    private final HealthStepQueryService queryService;

    @Operation(summary = "일별 걸음 활동 집계 조회")
    @GetMapping("/daily")
    public ResponseEntity<List<HealthStepDailyResponse>> getDaily(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam String recordKey,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth
    ) {
        return ok(queryService.getDaily(
            Long.valueOf(jwt.getSubject()),
            jwt.getClaimAsString(AccessTokenIssuer.RECORD_KEY_CLAIM),
            recordKey,
            yearMonth
        ));
    }

    @Operation(summary = "월별 걸음 활동 집계 조회")
    @GetMapping("/monthly")
    public ResponseEntity<List<HealthStepMonthlyResponse>> getMonthly(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam String recordKey,
        @RequestParam(required = false) Integer year
    ) {
        return ok(queryService.getMonthly(
            Long.valueOf(jwt.getSubject()),
            jwt.getClaimAsString(AccessTokenIssuer.RECORD_KEY_CLAIM),
            recordKey,
            year
        ));
    }
}
