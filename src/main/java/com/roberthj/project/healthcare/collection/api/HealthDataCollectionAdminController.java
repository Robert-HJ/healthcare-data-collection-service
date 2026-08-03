package com.roberthj.project.healthcare.collection.api;

import com.roberthj.project.healthcare.collection.response.HealthDataCollectionResponse;
import com.roberthj.project.healthcare.collection.service.HealthDataCollectionAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.roberthj.project.healthcare.common.response.ResponseEntityFactory.accepted;
import static com.roberthj.project.healthcare.common.response.ResponseEntityFactory.ok;

@Tag(name = "Health Data Collection Admin")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/health-data/collections")
public class HealthDataCollectionAdminController {

    private final HealthDataCollectionAdminService adminService;

    @Operation(summary = "건강 데이터 수집 요청 상태 조회")
    @GetMapping("/{requestId}")
    public ResponseEntity<HealthDataCollectionResponse> getStatus(@PathVariable Long requestId) {
        return ok(adminService.getStatus(requestId));
    }

    @Operation(summary = "건강 데이터 수집 요청 수동 재처리")
    @PostMapping("/{requestId}/reprocess")
    public ResponseEntity<Void> reprocess(@PathVariable Long requestId) {
        adminService.reprocess(requestId);
        return accepted();
    }
}
