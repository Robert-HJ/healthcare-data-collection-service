package com.roberthj.project.healthcare.collection.api;

import com.roberthj.project.healthcare.auth.component.AccessTokenIssuer;
import com.roberthj.project.healthcare.collection.response.HealthDataCollectionResponse;
import com.roberthj.project.healthcare.collection.service.HealthDataCollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@Tag(name = "Health Data Collection")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/health-data/collections")
public class HealthDataCollectionController {

    private final HealthDataCollectionService collectionService;

    @Operation(summary = "건강 활동 데이터 수집")
    @PostMapping
    public ResponseEntity<HealthDataCollectionResponse> collect(
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody JsonNode payload
    ) {
        HealthDataCollectionResponse response = collectionService.collect(
            Long.valueOf(jwt.getSubject()),
            jwt.getClaimAsString(AccessTokenIssuer.RECORD_KEY_CLAIM),
            payload
        );

        return ResponseEntity.accepted().body(response);
    }
}
