package com.anjing.aspect;

import com.anjing.annotation.DistributeLock;
import com.anjing.config.lock.LocalLockConfig.LocalLockManager;
import com.anjing.config.properties.FeatureProperties;
import com.anjing.model.errorcode.LockErrorCode;
import com.anjing.model.exception.SystemException;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DistributeLockAspectTest {
    private final FeatureProperties features = new FeatureProperties();
    private final StaticListableBeanFactory beans = new StaticListableBeanFactory();
    private final LocalLockManager locks = new LocalLockManager();

    @Test void disabledLocksNeedNoProviderAndPreserveBusinessExceptions() {
        features.getDistributedLock().setEnabled(false);
        Probe probe = proxy();
        assertThat(probe.missingKey()).isEqualTo("executed");
        assertThatThrownBy(probe::failure).isSameAs(probeFailure);
    }

    @Test void localProviderUsesExpressionWhenFixedKeyIsOmitted() {
        useLocal();
        assertThat(proxy().expression("order-1")).isTrue();
        assertThat(locks.getStats()).isEqualTo("总锁数: 0, 活跃锁: 0");
    }

    @Test void localProviderReleasesOnBusinessExceptionWithoutWrapping() {
        useLocal();
        assertThatThrownBy(() -> proxy().failure()).isSameAs(probeFailure);
        assertThat(locks.isLocked("probe#failure")).isFalse();
        assertThat(locks.getStats()).isEqualTo("总锁数: 0, 活跃锁: 0");
    }

    @Test void missingOrNullExpressionKeysAreRejectedBeforeBusinessRuns() {
        useLocal();
        assertCode(() -> proxy().missingKey(), LockErrorCode.LOCK_KEY_MISSING);
        assertCode(() -> proxy().expression(null), LockErrorCode.LOCK_KEY_MISSING);
        assertThat(locks.getStats()).isEqualTo("总锁数: 0, 活跃锁: 0");
    }

    @Test void enabledProviderFailsClosedIfInfrastructureIsMissing() {
        features.getDistributedLock().setProvider("local");
        assertCode(() -> proxy().failure(), LockErrorCode.LOCK_ACQUIRE_FAILED);
        features.getDistributedLock().setProvider("redisson");
        assertCode(() -> proxy().failure(), LockErrorCode.LOCK_REDIS_ERROR);
        features.getDistributedLock().setProvider("typo");
        assertCode(() -> proxy().failure(), LockErrorCode.LOCK_CONFIG_ERROR);
    }

    @Test void redissonProviderStillAcquiresAndReleasesAroundBusinessFailure() {
        RedissonClient redis = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        beans.addBean("redisson", redis);
        when(redis.getLock("probe#failure")).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        assertThatThrownBy(() -> proxy().failure()).isSameAs(probeFailure);
        verify(lock).lock();
        verify(lock).unlock();
    }

    private void useLocal() {
        features.getDistributedLock().setProvider("local");
        beans.addBean("local", locks);
    }

    private Probe proxy() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new Probe(locks));
        factory.addAspect(new DistributeLockAspect(beans.getBeanProvider(RedissonClient.class),
            beans.getBeanProvider(LocalLockManager.class), features));
        return factory.getProxy();
    }

    private void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, LockErrorCode code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(SystemException.class,
            error -> assertThat(error.getErrorCode()).isEqualTo(code));
    }

    private static final IllegalStateException probeFailure = new IllegalStateException("business failure");

    public static class Probe {
        private final LocalLockManager locks;
        public Probe(LocalLockManager locks) { this.locks = locks; }

        @DistributeLock(scene="probe", keyExpression="#orderId")
        public boolean expression(String orderId) { return locks.isLocked("probe#" + orderId); }

        @DistributeLock(scene="probe", key="failure")
        public String failure() { throw probeFailure; }

        @DistributeLock(scene="probe")
        public String missingKey() { return "executed"; }
    }
}
