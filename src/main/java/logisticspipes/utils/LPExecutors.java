package logisticspipes.utils;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

import net.neoforged.neoforge.server.ServerLifecycleHooks;

import org.jspecify.annotations.Nullable;

/**
 * Where Logistics Pipes runs work that is not on the server thread.
 */
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
        return ServerTickExecutor.INSTANCE.submit(supplier).join();
    }

    /**
     * Runs the task once the server has advanced by {@code inTicks} ticks.
     */
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
