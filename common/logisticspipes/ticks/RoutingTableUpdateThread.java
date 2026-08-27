package logisticspipes.ticks;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import logisticspipes.LPConfigs;
import logisticspipes.LPConstants;

public class RoutingTableUpdateThread extends Thread {

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

    @Override
    @SuppressWarnings("InfiniteLoopStatement") // the loop ends by interrupting the thread
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
        } catch (InterruptedException ignored) {
        }
    }
}
