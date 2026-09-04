package logisticspipes.ticks;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Work handed to the client thread to run on its next tick.
 *
 * <p>Chunk data arrives while the block entity is still being placed, so anything that touches the
 * level -- lighting a block update, rebuilding a model -- has to wait until the tick after.
 */
public final class ClientTaskQueue {

    private static final Queue<Runnable> QUEUE = new ArrayDeque<>();

    private ClientTaskQueue() {}

    public static void add(Runnable task) {
        synchronized (QUEUE) {
            QUEUE.add(task);
        }
    }

    /** Runs everything queued so far. Tasks added while running wait for the next tick. */
    public static void runQueued() {
        final Runnable[] tasks;
        synchronized (QUEUE) {
            if (QUEUE.isEmpty()) {
                return;
            }
            tasks = QUEUE.toArray(new Runnable[0]);
            QUEUE.clear();
        }
        for (Runnable task : tasks) {
            task.run();
        }
    }
}
