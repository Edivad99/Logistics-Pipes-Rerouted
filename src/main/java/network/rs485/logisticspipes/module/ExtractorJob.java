package network.rs485.logisticspipes.module;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import kotlin.Pair;

import logisticspipes.LPConfigs;
import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.particle.Particles;
import logisticspipes.routing.AsyncRouting;
import logisticspipes.routing.ServerRouter;
import logisticspipes.util.ModuleUtil;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.logistics.LogisticsManager;
import network.rs485.logisticspipes.util.ItemKt;

/**
 * One pass over the attached inventory, spread over several ticks: each tick looks at the next few
 * slots on the server thread, while the job waits off it for the routing table it needs.
 */
public class ExtractorJob {

    private final AsyncExtractorModule module;
    private final Supplier<@Nullable IInventoryUtil> inventoryGetter;
    private final int slotsPerTick;
    private final int lastStartSlot;
    private final RoutingUpdateRequests updateRoutingTableRequests = new RoutingUpdateRequests();
    private final Map<Integer, ItemIdentifierStack> slotItemsToExtract = new HashMap<>();

    private int inventorySize;
    private int nextStartSlot = 0;
    private int itemsLeft;
    private int stacksLeft;

    ExtractorJob(AsyncExtractorModule module, Supplier<@Nullable IInventoryUtil> inventoryGetter) {
        this.module = module;
        this.inventoryGetter = inventoryGetter;
        IInventoryUtil inventory = inventoryGetter.get();
        this.inventorySize = inventory == null ? 0 : inventory.getContainerSize();
        this.slotsPerTick = ModuleUtil.determineSlotsPerTick(module.getEveryNthTick(), inventorySize);
        // The slots to start from, stepping by slotsPerTick, fixed on the size seen right now.
        this.lastStartSlot = inventorySize - 1;
        this.itemsLeft = module.getItemsToExtract();
        this.stacksLeft = module.getStacksToExtract();
    }

    private boolean hasNextStartSlot() {
        return slotsPerTick > 0 && nextStartSlot <= lastStartSlot;
    }

    public void runSyncWork() {
        try {
            ServerRouter serverRouter = module.getServerRouter();
            IInventoryUtil inventory = inventoryGetter.get();
            if (slotsPerTick == 0 || !hasNextStartSlot() || serverRouter == null || inventory == null) {
                updateRoutingTableRequests.close();
                return;
            }
            inventorySize = inventory.getContainerSize();
            int startSlot = nextStartSlot;
            nextStartSlot = startSlot + slotsPerTick;
            int stopSlot = hasNextStartSlot() ? Math.min(inventorySize, startSlot + slotsPerTick) : inventorySize;

            for (int slot = startSlot; slot < stopSlot; ++slot) {
                ItemStack stack = inventory.getItem(slot);
                if (module.filtersOut(stack)) {
                    continue; // filters the stack out by the given filter method
                }
                int toExtract = Math.min(itemsLeft, stack.getCount());
                itemsLeft -= toExtract;
                --stacksLeft;
                slotItemsToExtract.put(slot, new ItemIdentifierStack(ItemIdentifier.get(stack), toExtract));
                if (itemsLeft < 1 || stacksLeft < 1) {
                    break;
                }
            }
            AsyncRouting.updateServerRouterLsa(serverRouter);
            if (AsyncRouting.needsRoutingTableUpdate(serverRouter)) {
                updateRoutingTableRequests.request();
            } else {
                extractAndSend(serverRouter, inventory);
            }
            if (!hasNextStartSlot()) {
                updateRoutingTableRequests.close();
            }
        } catch (RuntimeException error) {
            updateRoutingTableRequests.close(error);
            throw error;
        }
    }

    /** Waits for the slot scan to ask for a routing table, and builds it away from the server thread. */
    public void runAsyncWork() {
        if (LPConfigs.COMMON.DISABLE_ASYNC_WORK.getAsBoolean()) {
            return;
        }
        while (updateRoutingTableRequests.awaitRequest()) {
            ServerRouter serverRouter = module.getServerRouter();
            if (serverRouter != null) {
                AsyncRouting.updateRoutingTable(serverRouter);
            }
        }
    }

    public void extractAndSend(ServerRouter serverRouter, IInventoryUtil inventory) {
        slotItemsToExtract.forEach((slot, itemIdStack) ->
            extractAndSendStack(serverRouter, inventory, slot, itemIdStack));
        slotItemsToExtract.clear();
    }

    private void extractAndSendStack(ServerRouter serverRouter, IInventoryUtil inventory, int slot,
        ItemIdentifierStack itemIdStack) {
        IPipeServiceProvider service = module.getPipeService();
        if (service == null) {
            return;
        }
        Direction pointedOrientation = service.getPointedOrientation();
        if (pointedOrientation == null) {
            return;
        }
        ItemStack stack = inventory.getItem(slot);
        if (!ItemKt.equalsWithNBT(itemIdStack.getItem(), stack)) {
            return;
        }
        int[] sourceStackLeft = { itemIdStack.getStackSize() };
        Iterator<Pair<Integer, SinkReply>> validDestinations = LogisticsManager.INSTANCE.allDestinations(
            stack,
            ItemIdentifier.get(stack),
            true,
            serverRouter,
            () -> sourceStackLeft[0] > 0).iterator();
        while (validDestinations.hasNext()) {
            Pair<Integer, SinkReply> destination = validDestinations.next();
            SinkReply sinkReply = destination.getSecond();
            int extract = ItemKt.getExtractionMax(stack.getCount(), sourceStackLeft[0], sinkReply);
            if (extract < 1) {
                continue;
            }
            while (!service.useEnergy(module.getEnergyPerItem() * extract)) {
                service.spawnParticle(Particles.ORANGE_SPARKLE, 2);
                if (extract < 2) {
                    break;
                }
                extract /= 2;
            }
            ItemStack toSend = inventory.removeItem(slot, extract);
            if (toSend.isEmpty()) {
                continue;
            }
            service.sendStack(toSend, destination.getFirst(), sinkReply, module.getItemSendMode(), pointedOrientation);
            sourceStackLeft[0] -= toSend.getCount();
        }
    }

    /**
     * Hands the "the routing table is stale" request from the server thread to the waiting job. Only
     * the latest request is kept, since rebuilding once covers every request made meanwhile.
     *
     * <p>The job waits through a {@link ForkJoinPool.ManagedBlocker} so the pool running it makes up
     * for the parked thread: a job lives for as many ticks as the scan takes, and without that the
     * extractors would hold every worker the pool has.
     */
    private static final class RoutingUpdateRequests {

        private final Object lock = new Object();
        private boolean requested;
        private boolean closed;
        private @Nullable RuntimeException failure;

        void request() {
            synchronized (lock) {
                requested = true;
                lock.notifyAll();
            }
        }

        void close() {
            close(null);
        }

        void close(@Nullable RuntimeException error) {
            synchronized (lock) {
                closed = true;
                failure = error;
                lock.notifyAll();
            }
        }

        /**
         * Waits for the next request and returns true, or false once the scan is over. A request
         * made before the scan closed is still handed over, and a scan that ended in failure throws
         * it here, so the job fails with the same cause.
         */
        boolean awaitRequest() {
            try {
                ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker() {
                    @Override
                    public boolean block() throws InterruptedException {
                        synchronized (lock) {
                            while (!requested && !closed) {
                                lock.wait();
                            }
                        }
                        return true;
                    }

                    @Override
                    public boolean isReleasable() {
                        synchronized (lock) {
                            return requested || closed;
                        }
                    }
                });
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return false;
            }
            synchronized (lock) {
                if (requested) {
                    requested = false;
                    return true;
                }
                RuntimeException error = failure;
                if (error != null) {
                    throw new CompletionException(error);
                }
                return false;
            }
        }
    }
}
