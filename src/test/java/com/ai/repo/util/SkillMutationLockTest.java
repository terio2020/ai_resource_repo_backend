package com.ai.repo.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;

class SkillMutationLockTest {
    @Test void publicationLockRemainsHeldUntilTransactionCompletes() throws Exception {
        TransactionSynchronizationManager.initSynchronization();
        CountDownLatch attempted = new CountDownLatch(1);
        CountDownLatch acquired = new CountDownLatch(1);
        Thread contender = new Thread(() -> {
            attempted.countDown();
            try (SkillMutationLock ignored = SkillMutationLock.acquire(42L)) { acquired.countDown(); }
        });
        try {
            try (SkillMutationLock ignored = SkillMutationLock.acquire(42L)) { /* commit is deferred */ }
            contender.start();
            assertTrue(attempted.await(1, TimeUnit.SECONDS));
            assertFalse(acquired.await(100, TimeUnit.MILLISECONDS));
        } finally {
            for (var synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
            }
            TransactionSynchronizationManager.clearSynchronization();
        }
        assertTrue(acquired.await(1, TimeUnit.SECONDS));
        contender.join(1000);
    }
}
