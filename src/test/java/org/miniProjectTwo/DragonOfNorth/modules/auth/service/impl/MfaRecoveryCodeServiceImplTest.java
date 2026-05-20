package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserMfaRecoveryCode;
import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserMfaSettings;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserMfaRecoveryCodeRepository;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MfaRecoveryCodeServiceImplTest {

    private UserMfaRecoveryCodeRepository recoveryCodeRepository;
    private MfaRecoveryCodeServiceImpl recoveryCodeService;
    private PasswordEncoder passwordEncoder;
    private UserMfaSettings mfaSettings;

    @BeforeEach
    void setUp() {
        recoveryCodeRepository = mock(UserMfaRecoveryCodeRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        recoveryCodeService = new MfaRecoveryCodeServiceImpl(recoveryCodeRepository, passwordEncoder);

        mfaSettings = new UserMfaSettings();
        mfaSettings.setId(UUID.randomUUID());
    }

    @Test
    void generateAndStoreRecoveryCodes_shouldPersistOnlyHashes() {
        String[] plaintextCodes = recoveryCodeService.generateAndStoreRecoveryCodes(mfaSettings);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserMfaRecoveryCode>> captor = ArgumentCaptor.forClass(List.class);
        verify(recoveryCodeRepository).invalidateActiveCodes(eq(mfaSettings.getId()), any(Instant.class));
        verify(recoveryCodeRepository).saveAll(captor.capture());

        List<UserMfaRecoveryCode> persistedCodes = captor.getValue();
        assertThat(plaintextCodes).hasSize(10);
        assertThat(persistedCodes).hasSize(10);
        assertThat(persistedCodes)
                .allSatisfy(code -> {
                    assertThat(code.getMfaSettings()).isSameAs(mfaSettings);
                    assertThat(code.getRecoveryCodeHash()).startsWith("$2");
                    assertThat(plaintextCodes).doesNotContain(code.getRecoveryCodeHash());
                    assertThat(code.isUsed()).isFalse();
                });
    }

    @Test
    void verifyAndConsumeRecoveryCode_shouldMarkMatchingCodeUsed() {
        String hash = passwordEncoder.encode("ABCD-EFGH");
        UserMfaRecoveryCode recoveryCode = new UserMfaRecoveryCode(mfaSettings, hash);
        recoveryCode.setId(UUID.randomUUID());

        when(recoveryCodeRepository.findByMfaSettingsIdAndUsedFalseAndDeletedFalse(mfaSettings.getId()))
                .thenReturn(List.of(recoveryCode));
        when(recoveryCodeRepository.consumeIfUnused(eq(recoveryCode.getId()), any(Instant.class))).thenReturn(1);

        assertThat(recoveryCodeService.verifyAndConsumeRecoveryCode(mfaSettings, " abcd-efgh ")).isTrue();
        verify(recoveryCodeRepository).consumeIfUnused(eq(recoveryCode.getId()), any(Instant.class));
    }

    @Test
    void verifyAndConsumeRecoveryCode_shouldRejectMissingOrUsedCode() {
        when(recoveryCodeRepository.findByMfaSettingsIdAndUsedFalseAndDeletedFalse(mfaSettings.getId()))
                .thenReturn(List.of());

        assertThat(recoveryCodeService.verifyAndConsumeRecoveryCode(mfaSettings, "ABCD-EFGH")).isFalse();
    }

    @Test
    void verifyAndConsumeRecoveryCode_shouldAllowExactlyOneWinnerUnderConcurrency() throws Exception {
        String hash = passwordEncoder.encode("ABCD-EFGH");
        UserMfaRecoveryCode recoveryCode = new UserMfaRecoveryCode(mfaSettings, hash);
        recoveryCode.setId(UUID.randomUUID());

        when(recoveryCodeRepository.findByMfaSettingsIdAndUsedFalseAndDeletedFalse(mfaSettings.getId()))
                .thenReturn(List.of(recoveryCode));

        AtomicBoolean consumed = new AtomicBoolean(false);
        when(recoveryCodeRepository.consumeIfUnused(eq(recoveryCode.getId()), any(Instant.class)))
                .thenAnswer(invocation -> consumed.compareAndSet(false, true) ? 1 : 0);

        int requestCount = 8;
        ExecutorService pool = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger(0);

        try {
            List<Future<Boolean>> futures = java.util.stream.IntStream.range(0, requestCount)
                    .mapToObj(i -> pool.submit(() -> {
                        ready.countDown();
                        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                        return recoveryCodeService.verifyAndConsumeRecoveryCode(mfaSettings, "ABCD-EFGH");
                    }))
                    .toList();

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            for (Future<Boolean> future : futures) {
                if (future.get(5, TimeUnit.SECONDS)) {
                    successes.incrementAndGet();
                }
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(successes.get()).isEqualTo(1);
    }
}
