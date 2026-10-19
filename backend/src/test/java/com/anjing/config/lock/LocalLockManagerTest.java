package com.anjing.config.lock;

import com.anjing.config.lock.LocalLockConfig.LocalLockManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@Timeout(15)
class LocalLockManagerTest {
    private final LocalLockManager locks = new LocalLockManager();

    @Test void reentrantUnlockKeepsEntryUntilLastHoldIsReleased() throws Exception {
        assertThat(locks.tryLock("order", -1, -1)).isTrue();
        assertThat(locks.tryLock("order", -1, -1)).isTrue();
        locks.unlock("order");
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            assertThat(pool.submit(() -> locks.tryLock("order", 0, -1)).get(3, TimeUnit.SECONDS)).isFalse();
            assertThat(locks.isLocked("order")).isTrue();
        } finally {
            locks.unlock("order");
            pool.shutdownNow();
        }
        assertEmpty();
    }

    @Test void sameKeyRemainsExclusiveAcrossConcurrentAcquireAndCleanup() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger active = new AtomicInteger(), overlaps = new AtomicInteger();
        var futures = new ArrayList<Future<?>>();
        try {
            for (int worker = 0; worker < 8; worker++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    for (int iteration = 0; iteration < 1000; iteration++) {
                        if (!locks.tryLock("shared", -1, -1)) throw new AssertionError("lock not acquired");
                        try {
                            if (active.incrementAndGet() != 1) overlaps.incrementAndGet();
                            Thread.yield();
                            active.decrementAndGet();
                        } finally { locks.unlock("shared"); }
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) future.get(10, TimeUnit.SECONDS);
            assertThat(overlaps.get()).isZero();
            assertEmpty();
        } finally { start.countDown(); pool.shutdownNow(); }
    }

    @Test void defaultWaitBlocksAndAcquiresAfterOwnerReleases() throws Exception {
        locks.tryLock("order", -1, -1);
        CountDownLatch started = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> waiting = pool.submit(() -> {
                started.countDown();
                boolean acquired = locks.tryLock("order", -1, -1);
                if (acquired) locks.unlock("order");
                return acquired;
            });
            assertThat(started.await(3, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> waiting.get(100, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            locks.unlock("order");
            assertThat(waiting.get(3, TimeUnit.SECONDS)).isTrue();
            assertEmpty();
        } finally { locks.unlock("order"); pool.shutdownNow(); }
    }

    @Test void timedOutWaiterAndForeignUnlockCannotRemoveOwnersLock() throws Exception {
        locks.tryLock("order", -1, -1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            assertThat(pool.submit(() -> {
                locks.unlock("order");
                return locks.tryLock("order", 1, -1);
            }).get(3, TimeUnit.SECONDS)).isFalse();
            assertThat(locks.isLocked("order")).isTrue();
        } finally { locks.unlock("order"); pool.shutdownNow(); }
        assertEmpty();
    }

    @Test void interruptedWaiterRestoresInterruptAndReleasesItsReference() throws Exception {
        locks.tryLock("order", -1, -1);
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean acquired = new AtomicBoolean(true), interrupted = new AtomicBoolean();
        Thread waiter = new Thread(() -> {
            started.countDown();
            acquired.set(locks.tryLock("order", -1, -1));
            interrupted.set(Thread.currentThread().isInterrupted());
        });
        try {
            waiter.start();
            assertThat(started.await(3, TimeUnit.SECONDS)).isTrue();
            waiter.interrupt();
            waiter.join(3000);
            assertThat(waiter.isAlive()).isFalse();
            assertThat(acquired.get()).isFalse();
            assertThat(interrupted.get()).isTrue();
            assertThat(locks.isLocked("order")).isTrue();
        } finally { locks.unlock("order"); waiter.interrupt(); waiter.join(3000); }
        assertEmpty();
    }

    @Test void independentKeysCanBeHeldAtTheSameTimeAndAreReclaimed() throws Exception {
        locks.tryLock("first", -1, -1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            assertThat(pool.submit(() -> {
                boolean acquired = locks.tryLock("second", 0, -1);
                if (acquired) locks.unlock("second");
                return acquired;
            }).get(3, TimeUnit.SECONDS)).isTrue();
        } finally { locks.unlock("first"); pool.shutdownNow(); }
        assertThatThrownBy(() -> locks.tryLock("invalid", -2, -1)).isInstanceOf(IllegalArgumentException.class);
        assertEmpty();
    }

    private void assertEmpty() {
        assertThat(locks.getStats()).isEqualTo("总锁数: 0, 活跃锁: 0");
    }
}
