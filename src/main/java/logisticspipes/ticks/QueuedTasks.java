package logisticspipes.ticks;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import logisticspipes.LogisticsPipes;

/**
 * Work run on the server thread at the end of the tick, once the change that queued it is in
 * place. Tasks may be queued from any thread.
 */
public class QueuedTasks {

    private static final Queue<Runnable> QUEUE = new ConcurrentLinkedQueue<>();

    /**
     * Drops whatever is still queued, when the server stops.
     */
    public static void clearAllTasks() {
        QUEUE.clear();
    }

    public static void queueTask(Runnable task) {
        QUEUE.add(task);
    }

    /**
     * Runs everything queued, including the tasks those queue in turn.
     */
    @SubscribeEvent
    public void tickEnd(ServerTickEvent.Post event) {
        Runnable task;
        while ((task = QUEUE.poll()) != null) {
            try {
                task.run();
            } catch (Exception e) {
                LogisticsPipes.LOG.error("Exception in queued task", e);
            }
        }
    }
}
