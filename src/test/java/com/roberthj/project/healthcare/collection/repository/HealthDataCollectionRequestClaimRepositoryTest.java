package com.roberthj.project.healthcare.collection.repository;

import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequestEntity;
import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.collection.enums.HealthDataType;
import com.roberthj.project.healthcare.config.JpaConfig;
import com.roberthj.project.healthcare.member.entity.MemberEntity;
import com.roberthj.project.healthcare.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.node.JsonNodeFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
@Import({JpaConfig.class, HealthDataCollectionRequestClaimRepository.class})
class HealthDataCollectionRequestClaimRepositoryTest {

    private static final int MAX_RETRY_COUNT = 5;
    private static final Duration STALE_PROCESSING_TIMEOUT = Duration.ofMinutes(5);

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private HealthDataCollectionRequestRepository collectionRequestRepository;

    @Autowired
    private HealthDataCollectionRequestClaimRepository claimRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void doNotClaimNextRequestWhileOldestRequestInSameGroupIsProcessing() {
        MemberEntity member = saveMember();
        HealthDataCollectionRequestEntity oldestRequest = saveRequest(member);
        saveRequest(member);

        Optional<Long> firstClaimedRequestId = findNextRequest();
        claimRepository.updateProcessing(firstClaimedRequestId.orElseThrow());
        Optional<Long> secondClaimedRequestId = findNextRequest();

        assertThat(firstClaimedRequestId).contains(oldestRequest.getId());
        assertThat(secondClaimedRequestId).isEmpty();
    }

    @Test
    void claimNextRequestWhenPreviousFailureReachedRetryLimit() {
        MemberEntity member = saveMember();
        HealthDataCollectionRequestEntity failedRequest = saveRequest(member);
        HealthDataCollectionRequestEntity nextRequest = saveRequest(member);
        for (int count = 0; count < MAX_RETRY_COUNT; count++) {
            failedRequest.fail("processing failed");
        }
        collectionRequestRepository.flush();

        Optional<Long> claimedRequestId = findNextRequest();

        assertThat(claimedRequestId).contains(nextRequest.getId());
    }

    @Test
    void claimStaleProcessingRequestWithoutIncreasingRetryCount() {
        MemberEntity member = saveMember();
        HealthDataCollectionRequestEntity request = saveRequest(member);
        jdbcTemplate.update("""
            UPDATE health_data_collection_request
            SET status = 'PROCESSING', updated_at = DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 10 MINUTE)
            WHERE id = ?
            """, request.getId());

        Optional<Long> claimedRequestId = findNextRequest();
        claimRepository.updateProcessing(claimedRequestId.orElseThrow());
        entityManager.clear();

        HealthDataCollectionRequestEntity claimedRequest = collectionRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(claimedRequestId).contains(request.getId());
        assertThat(claimedRequest.getRetryCount()).isZero();
    }

    @Test
    void claimPendingRequestForManualReprocessing() {
        HealthDataCollectionRequestEntity request = saveRequest(saveMember());

        boolean claimed = claimRepository.claimForManualReprocessing(request.getId(), STALE_PROCESSING_TIMEOUT);

        assertThat(claimed).isTrue();
        assertThat(findStatus(request.getId())).isEqualTo("PROCESSING");
    }

    @Test
    void doNotClaimRecentlyProcessingRequestForManualReprocessing() {
        HealthDataCollectionRequestEntity request = saveRequest(saveMember());
        claimRepository.updateProcessing(request.getId());

        boolean claimed = claimRepository.claimForManualReprocessing(request.getId(), STALE_PROCESSING_TIMEOUT);

        assertThat(claimed).isFalse();
    }

    @Test
    void claimStaleProcessingRequestForManualReprocessing() {
        HealthDataCollectionRequestEntity request = saveRequest(saveMember());
        jdbcTemplate.update("""
            UPDATE health_data_collection_request
            SET status = 'PROCESSING', updated_at = DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 10 MINUTE)
            WHERE id = ?
            """, request.getId());

        boolean claimed = claimRepository.claimForManualReprocessing(request.getId(), STALE_PROCESSING_TIMEOUT);

        assertThat(claimed).isTrue();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void skipLockedRequestAndDoNotClaimNextRequestInSameGroup() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstRequestLocked = new CountDownLatch(1);
        CountDownLatch releaseFirstRequest = new CountDownLatch(1);
        List<Long> requestIds = new ArrayList<>();
        List<Long> memberIds = new ArrayList<>();

        try {
            MemberEntity firstMember = saveMember();
            memberIds.add(firstMember.getId());
            HealthDataCollectionRequestEntity firstRequest = saveRequest(firstMember);
            HealthDataCollectionRequestEntity nextRequestInSameGroup = saveRequest(firstMember);
            requestIds.add(firstRequest.getId());
            requestIds.add(nextRequestInSameGroup.getId());

            Future<Long> lockedRequestId = executor.submit(() -> executeInTransaction(() -> {
                Long requestId = findNextRequest().orElseThrow();
                firstRequestLocked.countDown();
                await(releaseFirstRequest);
                return requestId;
            }));

            assertThat(firstRequestLocked.await(3, TimeUnit.SECONDS)).isTrue();

            MemberEntity secondMember = saveMember();
            memberIds.add(secondMember.getId());
            HealthDataCollectionRequestEntity otherGroupRequest = saveRequest(secondMember);
            requestIds.add(otherGroupRequest.getId());

            Future<Optional<Long>> secondClaim = executor.submit(() -> executeInTransaction(this::findNextRequest));
            Optional<Long> secondClaimedRequestId = secondClaim.get(3, TimeUnit.SECONDS);

            assertThat(lockedRequestId.isDone()).isFalse();
            assertThat(secondClaimedRequestId).contains(otherGroupRequest.getId());
            assertThat(secondClaimedRequestId).isNotEqualTo(Optional.of(nextRequestInSameGroup.getId()));

            releaseFirstRequest.countDown();
            assertThat(lockedRequestId.get(3, TimeUnit.SECONDS)).isEqualTo(firstRequest.getId());
        } finally {
            releaseFirstRequest.countDown();
            executor.shutdownNow();
            executor.awaitTermination(3, TimeUnit.SECONDS);
            collectionRequestRepository.deleteAllByIdInBatch(requestIds);
            memberRepository.deleteAllByIdInBatch(memberIds);
        }
    }

    private Optional<Long> findNextRequest() {
        return claimRepository.findNextRequest(MAX_RETRY_COUNT, STALE_PROCESSING_TIMEOUT);
    }

    private String findStatus(Long requestId) {
        return jdbcTemplate.queryForObject("SELECT status FROM health_data_collection_request WHERE id = ?", String.class, requestId);
    }

    private <T> T executeInTransaction(Supplier<T> action) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return transactionTemplate.execute(status -> action.get());
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(3, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시성 테스트 대기 시간이 초과되었습니다.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시성 테스트 대기 중 인터럽트가 발생했습니다.", exception);
        }
    }

    private MemberEntity saveMember() {
        String recordKey = UUID.randomUUID().toString();
        return memberRepository.saveAndFlush(MemberEntity.create(
            "홍길동",
            "길동",
            "member-" + recordKey + "@example.com",
            "encoded-password",
            recordKey
        ));
    }

    private HealthDataCollectionRequestEntity saveRequest(MemberEntity member) {
        return collectionRequestRepository.saveAndFlush(HealthDataCollectionRequestEntity.create(
            member,
            HealthDataType.STEPS,
            HealthDataSource.SAMSUNG_HEALTH,
            JsonNodeFactory.instance.objectNode()
        ));
    }
}
