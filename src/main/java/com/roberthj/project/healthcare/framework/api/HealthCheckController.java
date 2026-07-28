package com.roberthj.project.healthcare.framework.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

import static com.roberthj.project.healthcare.framework.response.ResponseEntityFactory.ok;

@Tag(name = "Health Check")
@RestController
public class HealthCheckController {

    @Operation(summary = "헬스 체크")
    @GetMapping("/health-check")
    public ResponseEntity<HealthCheckResponse> healthCheck() {
        return ok(new HealthCheckResponse("UP", Instant.now()));
    }

    public record HealthCheckResponse(String status, Instant timestamp) {}
}
