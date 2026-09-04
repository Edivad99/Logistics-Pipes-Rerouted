package logisticspipes.ticks;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import logisticspipes.LPConfigs;
import logisticspipes.LPConstants;

/**
 * One of the worker threads that rebuild routing tables off the server thread.
 *
 * <p>Final on purpose: the constructor ends with {@code start()}, so from that point another
 * thread is running {@link #run()} on an object whose construction has not returned. That is safe
 * only because {@code run()} touches nothing but the static queue and average -- a subclass would
 * find its own fields still unset.
 */
public final class RoutingTableUpdateThread extends Thread {

    private static final PriorityBlockingQueue<Runnable> UPDATE_CALLS = new PriorityBlockingQueue<>();
    // Several of these threads run at once, so this rolling average is a read-modify-write from
    // more than one thread. It must not go back to a Long guarded by synchronized(average):
    // every assignment autoboxes a new Long, so the monitor changed under the lock.
    private static final AtomicLong AVERAGE = new AtomicLong();

    public RoutingTableUpdateThread(int i) {
        super("[%s] RoutingTableUpdateThread #%d".formatted(LPConstants.NAME, i));
        setDaemon(true);
        setPriority(LPConfigs.COMMON.MULTI_THREAD_PRIORITY.getAsInt());
        start();
    }

    public static void add(Runnable run) {
        RoutingTableUpdateThread.UPDATE_CALLS.add(run);
    }

    public static boolean remove(Runnable run) {
        return RoutingTableUpdateThread.UPDATE_CALLS.remove(run);
    }

    public static int size() {
        return RoutingTableUpdateThread.UPDATE_CALLS.size();
    }

    public static long getAverage() {
        return RoutingTableUpdateThread.AVERAGE.get();
    }

    /**
     * Takes routing work off the queue for as long as the game runs.
     *
     * <p>Nothing ever stops these threads: they are started from common setup, nobody keeps a
     * reference and nothing calls {@code interrupt}. They are daemons, so the JVM exits without
     * waiting for them, and the loop below has no exit that is ever taken. The interrupt is
     * still handled rather than swallowed silently, so that a future shutdown path -- or a thread
     * dump taken while debugging -- finds the flag where it belongs instead of lost.
     */
    @Override
    @SuppressWarnings("InfiniteLoopStatement") // there is no shutdown; see above
    public void run() {
        try {
            // take() blocks until something is available and never returns null
            while (true) {
                Runnable item = RoutingTableUpdateThread.UPDATE_CALLS.take();
                long startTime = System.nanoTime();
                item.run();
                long took = System.nanoTime() - startTime;
                RoutingTableUpdateThread.AVERAGE.updateAndGet(
                    current -> current == 0 ? took : ((current * 999L) + took) / 1000L);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
