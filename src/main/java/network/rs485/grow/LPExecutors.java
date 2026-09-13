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

package network.rs485.grow;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.Nullable;

/** Where Logistics Pipes runs work that is not on the server thread. */
public final class LPExecutors {

    /**
     * Its own pool rather than the common one: jobs here block waiting for the server tick, and a
     * ForkJoinPool compensates for that by starting a replacement thread instead of starving.
     */
    private static final ForkJoinPool ASYNC_POOL = new ForkJoinPool(
        Math.max(2, Runtime.getRuntime().availableProcessors()),
        ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);

    private LPExecutors() {
    }

    public static Executor async() {
        return ASYNC_POOL;
    }

    public static ServerTickExecutor server() {
        return ServerTickExecutor.INSTANCE;
    }

    /**
     * Runs the supplier on the server thread and waits for its result.
     *
     * <p>For the step of a background job that has to touch state the server thread owns. Returns
     * straight away when it is already on the server thread, which would otherwise deadlock.
     */
    public static <T extends @Nullable Object> T onServerThread(Supplier<T> supplier) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isSameThread()) {
            return supplier.get();
        }
        return CompletableFuture.supplyAsync(supplier, ServerTickExecutor.INSTANCE).join();
    }

    /** Runs the task once the server has advanced by {@code inTicks} ticks. */
    public static void scheduleServerTask(int inTicks, Runnable task) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            throw new IllegalStateException("Cannot schedule task: server is not running");
        }
        int runTick = server.getTickCount() + inTicks;
        ServerTickExecutor.INSTANCE.execute(new Runnable() {
            @Override
            public void run() {
                if (server.getTickCount() >= runTick) {
                    task.run();
                } else {
                    ServerTickExecutor.INSTANCE.scheduleNextTick(this);
                }
            }
        });
    }
}
