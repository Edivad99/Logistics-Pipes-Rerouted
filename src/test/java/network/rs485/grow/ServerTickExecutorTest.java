package network.rs485.grow;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The contract the async modules rely on: work handed to the executor runs on the thread that
 * ticks it, and never before that tick.
 */
class ServerTickExecutorTest {

    private final ServerTickExecutor executor = ServerTickExecutor.INSTANCE;

    @BeforeEach
    void emptyTheQueues() {
        executor.cleanup();
    }

    @Test
    void executeDefersTheWorkToTheNextTick() {
        AtomicBoolean ran = new AtomicBoolean(false);
        executor.execute(() -> ran.set(true));

        assertFalse(ran.get(), "submitting must not run the work");

        executor.tick();
        assertTrue(ran.get(), "the tick must run what was submitted");
    }

    @Test
    void scheduleNextTickSkipsTheTickItWasQueuedFrom() {
        AtomicBoolean ran = new AtomicBoolean(false);
        executor.scheduleNextTick(() -> ran.set(true));

        executor.tick();
        assertFalse(ran.get(), "work scheduled for the next tick must not run in this one");

        executor.tick();
        assertTrue(ran.get(), "it must run on the tick after");
    }

    @Test
    void workRunsOnTheTickingThread() {
        AtomicReference<Thread> ranOn = new AtomicReference<>();
        executor.execute(() -> ranOn.set(Thread.currentThread()));

        executor.tick();

        assertSame(Thread.currentThread(), ranOn.get(), "work must run on the thread that ticks");
    }

    @Test
    void cleanupDropsPendingWork() {
        AtomicBoolean ran = new AtomicBoolean(false);
        executor.execute(() -> ran.set(true));
        executor.scheduleNextTick(() -> ran.set(true));

        executor.cleanup();
        executor.tick();
        executor.tick();

        assertFalse(ran.get(), "cleanup must drop what was still queued");
    }

    /**
     * The translation of the old {@code withContext(serverScope)}: a background job asks for a step
     * to happen on the server thread and waits for its result.
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void onServerThreadWaitsForTheTickAndHandsBackTheValue() throws Exception {
        CountDownLatch asked = new CountDownLatch(1);
        AtomicReference<Thread> ranOn = new AtomicReference<>();

        CompletableFuture<String> answer = CompletableFuture.supplyAsync(() -> {
            asked.countDown();
            return LPExecutors.onServerThread(() -> {
                ranOn.set(Thread.currentThread());
                return "from the tick";
            });
        });

        assertTrue(asked.await(5, TimeUnit.SECONDS), "the background job should have started");
        Thread ticking = Thread.currentThread();
        while (!answer.isDone()) {
            executor.tick();
            Thread.onSpinWait();
        }

        assertEquals("from the tick", answer.get(), "the value must come back to the caller");
        assertSame(ticking, ranOn.get(), "the step must have run on the ticking thread");
    }

    /**
     * Null has to survive the trip: it is why the supplier's type parameter is allowed to be a
     * nullable one.
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void onServerThreadPassesNullThrough() throws Exception {
        CompletableFuture<String> answer =
            CompletableFuture.supplyAsync(() -> LPExecutors.onServerThread(() -> (String) null));

        while (!answer.isDone()) {
            executor.tick();
            Thread.onSpinWait();
        }

        assertNull(answer.get(), "a null result must not be turned into anything else");
    }
}
