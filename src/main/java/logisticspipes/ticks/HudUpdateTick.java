package logisticspipes.ticks;

import java.util.BitSet;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.RouterManager;

/**
 * Sends the players watching a chassis the contents of its inventories when they change, spread
 * over the ticks so a lot of changes at once do not all go out in the same one.
 */
public final class HudUpdateTick {

    /**
     * The simple ids of the routers whose pipes have changes to send.
     */
    private static final BitSet ROUTERS_NEEDING_UPDATE = new BitSet(4096);

    /**
     * Inventory slots sent per tick at most: the router that goes over it still sends all of its own.
     */
    private static final int SLOTS_PER_TICK = 90;

    /**
     * The router to send next, or -1 when none is waiting.
     */
    private static int nextRouter = -1;

    private HudUpdateTick() {
    }

    public static void clearUpdateFlags() {
        ROUTERS_NEEDING_UPDATE.clear();
        nextRouter = -1;
    }

    public static void add(IRouter router) {
        int index = router.getSimpleID();
        if (index < 0) {
            return;
        }
        // Grows the bit set when the index is past its end.
        ROUTERS_NEEDING_UPDATE.set(index);
        if (nextRouter == -1) {
            nextRouter = index;
        }
    }

    public static void tick() {
        int slotsSent = 0;
        while (nextRouter != -1 && slotsSent < SLOTS_PER_TICK) {
            ROUTERS_NEEDING_UPDATE.clear(nextRouter);
            IRouter router = RouterManager.get().getServerRouter(nextRouter);
            if (router != null) {
                CoreRoutedPipe pipe = router.getCachedPipe();
                if (pipe != null) {
                    slotsSent += pipe.sendQueueChanged(true);
                }
            }
            // Routers added below the current one while it went round are picked up from the start.
            int next = ROUTERS_NEEDING_UPDATE.nextSetBit(nextRouter);
            nextRouter = next != -1 ? next : ROUTERS_NEEDING_UPDATE.nextSetBit(0);
        }
    }
}
