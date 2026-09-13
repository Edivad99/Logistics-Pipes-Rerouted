/*
 * Copyright (c) 2021  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2021  RS485
 *
 * This MIT license was reworded to only match this file. If you use the regular
 * MIT license in your project, replace this copyright notice (this line and any
 * lines below and NOT the copyright line above) with the lines from the original
 * MIT license located here: http://opensource.org/licenses/MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this file and associated documentation files (the "Source Code"), to deal in
 * the Source Code without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Source Code, and to permit persons to whom the Source Code is furnished
 * to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Source Code, which also can be
 * distributed under the MIT.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package logisticspipes.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import logisticspipes.LogisticsPipes;

/**
 * Work that has to happen on the server thread.
 *
 * <p>Submitting only queues; the queue is drained from the server tick, so anything handed here
 * runs where the world and the routers can be touched safely.
 */
public final class ServerTickExecutor implements Executor {

    /** Gives up draining after this long, so a runaway job cannot freeze the server. */
    private static final long TICK_BUDGET_NANOS = 1_000_000_000L;

    public static final ServerTickExecutor INSTANCE = new ServerTickExecutor();

    private final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();

    /** Guarded by itself; moved into the queue at the end of each tick. */
    private final List<Runnable> toSchedule = new ArrayList<>();

    /** Callers parked on a tick that has not happened yet, so {@link #cleanup} can let them go. */
    private final Set<CompletableFuture<?>> waitingForATick = ConcurrentHashMap.newKeySet();

    private ServerTickExecutor() {
    }

    @Override
    public void execute(Runnable block) {
        queue.add(block);
    }

    /**
     * Queues work whose result someone is waiting for. The returned future is completed from the
     * tick that runs it, fails with whatever the work threw, and is cancelled by {@link #cleanup}
     * so nobody stays parked on a tick that is no longer coming.
     */
    public <T extends @Nullable Object> CompletableFuture<T> submit(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        waitingForATick.add(future);
        future.whenComplete((value, error) -> waitingForATick.remove(future));
        queue.add(() -> {
            try {
                future.complete(supplier.get());
            } catch (RuntimeException error) {
                future.completeExceptionally(error);
            }
        });
        return future;
    }

    public boolean hasPendingWork() {
        return !queue.isEmpty();
    }

    /** Runs the block on one of the following ticks rather than this one. */
    public void scheduleNextTick(Runnable block) {
        synchronized (toSchedule) {
            toSchedule.add(block);
        }
    }

    public void tick() {
        long start = System.nanoTime();
        while (!queue.isEmpty() && (System.nanoTime() - start) < TICK_BUDGET_NANOS) {
            Runnable work = queue.poll();
            try {
                work.run();
            } catch (RuntimeException error) {
                // One bad task is not a reason to drop the rest, let alone to break the server tick.
                LogisticsPipes.LOG.error("Error running Logistics Pipes work on the server thread", error);
            }
        }
        if (System.nanoTime() - start >= TICK_BUDGET_NANOS) {
            LogisticsPipes.LOG.warn("Logistics Pipes server tick work hung for a second. Remaining work: {}", queue);
        }
        synchronized (toSchedule) {
            queue.addAll(toSchedule);
            toSchedule.clear();
        }
    }

    public void cleanup() {
        queue.clear();
        synchronized (toSchedule) {
            toSchedule.clear();
        }
        List<CompletableFuture<?>> parked = new ArrayList<>(waitingForATick);
        waitingForATick.clear();
        parked.forEach(future ->
            future.completeExceptionally(new CancellationException("the server stopped before this work ran")));
    }
}
