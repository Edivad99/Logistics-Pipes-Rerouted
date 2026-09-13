package logisticspipes.modules;

import java.lang.reflect.Proxy;

import network.rs485.logisticspipes.connection.NoAdjacent;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.api.property.Property;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tick lifecycle every async module relies on: plan off the server thread, apply on it, one
 * job at a time.
 */
@Timeout(value = 20, unit = TimeUnit.SECONDS)
class AsyncModuleTest {

    @Test
    void noJobStartsOnAnOrdinaryTick() {
        TestModule module = new TestModule();
        module.nthTick.set(false);

        module.tick();

        assertEquals(0, module.setupCount.get(), "jobSetup must not run outside the Nth tick");
        assertEquals(0, module.syncWorkCount.get(), "there is no job, so there is no sync work");
    }

    @Test
    void theSetupRunsOnTheTickingThreadAndTheJobDoesNot() {
        TestModule module = new TestModule();
        module.nthTick.set(true);

        module.tick();

        assertEquals(1, module.setupCount.get());
        assertSame(Thread.currentThread(), module.setupThread.get(), "jobSetup belongs to the server thread");

        module.nthTick.set(false);
        module.release.countDown();
        tickUntil(module, () -> module.completed.get() > 0);

        assertNotSame(Thread.currentThread(), module.asyncThread.get(), "tickAsync must run off the server thread");
    }

    @Test
    void aRunningJobGetsSyncWorkAndNoSecondJob() throws Exception {
        TestModule module = new TestModule();
        module.nthTick.set(true);

        module.tick();                      // starts the job, which blocks on the latch
        module.tick();
        module.tick();

        assertEquals(1, module.setupCount.get(), "only one job may be in flight");
        assertTrue(module.syncWorkCount.get() >= 2, "each tick of a running job does sync work");
        assertEquals(0, module.completed.get(), "an unfinished job must not be completed");

        module.nthTick.set(false);
        module.release.countDown();
        tickUntil(module, () -> module.completed.get() > 0);
    }

    @Test
    void theResultReachesCompleteJobOnTheTickAfterTheJobEnds() {
        TestModule module = new TestModule();
        module.nthTick.set(true);

        module.tick();
        module.nthTick.set(false);          // so no further tick starts another job
        module.release.countDown();
        tickUntil(module, () -> module.completed.get() > 0);

        assertEquals(1, module.completed.get());
        assertEquals("worked on setup", module.completedWith.get(), "completeJob gets what tickAsync returned");
        assertSame(Thread.currentThread(), module.completeThread.get(), "completeJob belongs to the server thread");
    }

    @Test
    void aFailedJobCompletesWithNullInsteadOfEscaping() {
        TestModule module = new TestModule();
        module.failAsync.set(true);
        module.nthTick.set(true);

        module.tick();
        module.nthTick.set(false);
        module.release.countDown();
        tickUntil(module, () -> module.completed.get() > 0);   // must not throw

        assertEquals(1, module.completed.get(), "a failed job still reaches completeJob");
        assertNull(module.completedWith.get(), "a failed job hands over null");
    }

    @Test
    void afterAJobCompletesAnotherMayStart() {
        TestModule module = new TestModule();
        module.nthTick.set(true);
        module.release.countDown();         // every job of this test finishes straight away

        tickUntil(module, () -> module.setupCount.get() >= 2);

        assertTrue(module.setupCount.get() >= 2, "a finished job frees the slot for the next one");
        assertTrue(module.completed.get() >= 1, "and the finished one was completed on the way");
    }

    @Test
    void aFailureWhoseDiagnosticsAlsoFailStillCompletesWithNull() {
        TestModule module = new TestModule();
        module.failAsync.set(true);
        module.adjacentIsGone.set(true);    // as if the pipe went away while the job ran
        module.nthTick.set(true);

        module.tick();
        module.nthTick.set(false);
        module.release.countDown();
        tickUntil(module, () -> module.completed.get() > 0);

        assertEquals(1, module.completed.get(), "reporting trouble must not strand the job");
        assertNull(module.completedWith.get());
    }

    /** Ticks the module until the condition holds, the way the server drives it. */
    private static void tickUntil(TestModule module, java.util.function.BooleanSupplier done) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!done.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("the module never reached the expected state");
            }
            module.tick();
            Thread.onSpinWait();
        }
    }

    /** A module whose async work waits on a latch, so the test decides when it ends. */
    private static final class TestModule extends AsyncModule<String, String> {

        final AtomicBoolean nthTick = new AtomicBoolean(false);
        final AtomicBoolean adjacentIsGone = new AtomicBoolean(false);
        final AtomicBoolean failAsync = new AtomicBoolean(false);

        final AtomicInteger setupCount = new AtomicInteger();
        final AtomicInteger syncWorkCount = new AtomicInteger();
        final AtomicInteger completed = new AtomicInteger();

        final AtomicReference<@Nullable Thread> setupThread = new AtomicReference<>();
        final AtomicReference<@Nullable Thread> asyncThread = new AtomicReference<>();
        final AtomicReference<@Nullable Thread> completeThread = new AtomicReference<>();
        final AtomicReference<@Nullable String> completedWith = new AtomicReference<>();

        final CountDownLatch release = new CountDownLatch(1);

        TestModule() {
            this.service = fakeService(nthTick, adjacentIsGone);
        }

        @Override
        public int getEveryNthTick() {
            return 1;
        }

        @Override
        public String jobSetup() {
            setupCount.incrementAndGet();
            setupThread.set(Thread.currentThread());
            return "setup";
        }

        @Override
        public String tickAsync(String setupObject) {
            asyncThread.set(Thread.currentThread());
            try {
                release.await(10, TimeUnit.SECONDS);
                if (failAsync.get()) {
                    throw new IllegalStateException("deliberate failure");
                }
                return "worked on " + setupObject;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void completeJob(@Nullable String result) {
            completed.incrementAndGet();
            completedWith.set(result);
            completeThread.set(Thread.currentThread());
        }

        @Override
        public void runSyncWork() {
            syncWorkCount.incrementAndGet();
        }

        @Override
        public String getLPName() {
            return "test.async";
        }

        @Override
        public List<Property<?>> getProperties() {
            return List.of();
        }

        @Override
        public boolean hasGenericInterests() {
            return false;
        }

        @Override
        public boolean interestedInAttachedInventory() {
            return false;
        }

        @Override
        public boolean interestedInUndamagedID() {
            return false;
        }

        @Override
        public boolean receivePassive() {
            return false;
        }
    }

    /** Answers isNthTick from the flag and gives harmless defaults for everything else. */
    private static IPipeServiceProvider fakeService(AtomicBoolean nthTick, AtomicBoolean adjacentIsGone) {
        return (IPipeServiceProvider) Proxy.newProxyInstance(
            AsyncModuleTest.class.getClassLoader(),
            new Class<?>[] { IPipeServiceProvider.class },
            (proxy, method, args) -> {
                if (method.getName().equals("isNthTick")) {
                    return nthTick.get();
                }
                if (method.getName().equals("getAvailableAdjacent")) {
                    if (adjacentIsGone.get()) {
                        throw new IllegalStateException("the pipe is gone");
                    }
                    return NoAdjacent.INSTANCE;
                }
                Class<?> returnType = method.getReturnType();
                if (returnType == boolean.class) {
                    return false;
                }
                if (returnType.isPrimitive() && returnType != void.class) {
                    return 0;
                }
                return null;
            });
    }
}
