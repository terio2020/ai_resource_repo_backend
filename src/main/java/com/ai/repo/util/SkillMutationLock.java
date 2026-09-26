package com.ai.repo.util;

import java.util.concurrent.locks.ReentrantLock;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Coordinates Git and SQL visibility changes on the current single application instance. */
public final class SkillMutationLock implements AutoCloseable {
    private static final ReentrantLock[] LOCKS = new ReentrantLock[64];
    static { for (int i = 0; i < LOCKS.length; i++) LOCKS[i] = new ReentrantLock(); }
    private final ReentrantLock lock;
    private final boolean deferred;

    private SkillMutationLock(Long repositoryId) {
        lock = LOCKS[Math.floorMod(repositoryId.hashCode(), LOCKS.length)];
        lock.lock();
        deferred = TransactionSynchronizationManager.isSynchronizationActive();
        if (deferred) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) { lock.unlock(); }
            });
        }
    }
    public static SkillMutationLock acquire(Long repositoryId) { return new SkillMutationLock(repositoryId); }
    @Override public void close() { if (!deferred) lock.unlock(); }
}
