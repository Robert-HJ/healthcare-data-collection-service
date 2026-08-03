package com.roberthj.project.healthcare.collection.api;

import com.roberthj.project.healthcare.auth.security.AccessTokenIssuer;
import com.roberthj.project.healthcare.collection.exception.CollectionException;
import com.roberthj.project.healthcare.collection.response.HealthDataCollectionResponse;
import com.roberthj.project.healthcare.collection.service.HealthDataCollectionAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.roberthj.project.healthcare.collection.enums.CollectionRequestStatus.FAILED;
import static com.roberthj.project.healthcare.collection.exception.CollectionErrorCode.COLLECTION_REQUEST_NOT_FOUND;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class HealthDataCollectionAdminControllerTest {

    private static final String API_PATH = "/api/admin/health-data/collections";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccessTokenIssuer accessTokenIssuer;

    @MockitoBean
    private HealthDataCollectionAdminService adminService;

    @Test
    void returnCollectionRequestStatus() throws Exception {
        when(adminService.getStatus(1L)).thenReturn(new HealthDataCollectionResponse(1L, FAILED));

        mockMvc.perform(get(API_PATH + "/1").header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requestId").value(1L))
            .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void acceptManualReprocessingRequest() throws Exception {
        mockMvc.perform(post(API_PATH + "/1/reprocess").header("Authorization", bearerToken()))
            .andExpect(status().isAccepted());

        verify(adminService).reprocess(1L);
    }

    @Test
    void returnNotFoundWhenCollectionRequestDoesNotExist() throws Exception {
        doThrow(new CollectionException(COLLECTION_REQUEST_NOT_FOUND, "1")).when(adminService).getStatus(1L);

        mockMvc.perform(get(API_PATH + "/1").header("Authorization", bearerToken()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("COLLECTION005"));
    }

    private String bearerToken() {
        return "Bearer " + accessTokenIssuer.issue(1L, "record-key").value();
    }
}
