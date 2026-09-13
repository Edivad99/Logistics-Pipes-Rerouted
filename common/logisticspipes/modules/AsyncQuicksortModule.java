package logisticspipes.modules;

import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.PacketDistributor;

import org.jspecify.annotations.Nullable;

import logisticspipes.LPConfigs;
import logisticspipes.api.property.Property;
import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.logistics.PassiveSinkFinder;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.to_client.module.QuickSortStateMessage;
import logisticspipes.particle.Particles;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.AsyncRouting;
import logisticspipes.routing.ServerRouter;
import logisticspipes.utils.ItemUtil;
import logisticspipes.utils.LPExecutors;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifier;

public class AsyncQuicksortModule extends AsyncModule<@Nullable AsyncQuicksortModule.QuicksortSetup,
    @Nullable AsyncQuicksortModule.QuicksortResult> {

    public static final int STALLED_DELAY = 24;
    public static final int NORMAL_DELAY = 6;

    private static final String NAME = "quick_sort";

    /** The slot and what sat in it when the job was planned. */
    public record QuicksortSetup(int slot, ItemStack stack) {}

    /** Where the planned stack is headed, as worked out away from the server thread. */
    public record QuicksortResult(int slot, ItemIdentifier itemid, int destRouterId, SinkReply sinkReply) {}

    private final PlayerCollectionList localSlotWatchers = new PlayerCollectionList();
    private boolean stalled = true;
    /**
     * The slot the next tick will look at. Watchers are told which slot is being *worked on*, so
     * the message is sent where the work starts and not from a setter here: an increment goes
     * through the setter too, and would announce the slot after the one in hand -- leaving the
     * first slot of the inventory announced and overwritten within the same tick, so never drawn.
     */
    private int currentSlot = 0;
    private int stallSlot = 0;

    public static String getName() {
        return NAME;
    }

    @Override
    public List<Property<?>> getProperties() {
        return List.of();
    }

    private int getEnergyPerStack() {
        return 500 + 1000 * getUpgradeManager().getItemStackExtractionUpgrade();
    }

    @Override
    public int getEveryNthTick() {
        return stalled ? STALLED_DELAY : NORMAL_DELAY;
    }

    @Override
    public String getLPName() {
        return NAME;
    }

    @Override
    public @Nullable QuicksortSetup jobSetup() {
        IPipeServiceProvider service = this.service;
        if (service == null || !(service.getRouter() instanceof ServerRouter serverRouter)) {
            return null;
        }
        List<@Nullable IInventoryUtil> inventories = PipeServiceProviderUtil.availableInventories(service);
        IInventoryUtil inventory = inventories.isEmpty() ? null : inventories.getFirst();
        if (inventory == null || inventory.getContainerSize() == 0) {
            return null;
        }
        if (currentSlot >= inventory.getContainerSize()) {
            currentSlot = 0;
        }
        int slot = currentSlot;
        currentSlot = slot + 1;
        notifyWorkingSlot(slot);
        ItemStack stack = inventory.getItem(slot);
        if (!stalled && slot == stallSlot) {
            stalled = true;
        }
        if (stack.isEmpty()) {
            return null;
        }
        AsyncRouting.updateServerRouterLsa(serverRouter);
        if (!LPConfigs.COMMON.DISABLE_ASYNC_WORK.getAsBoolean()
            && AsyncRouting.needsRoutingTableUpdate(serverRouter)) {
            // go async
            return new QuicksortSetup(slot, stack);
        }
        ItemIdentifier itemid = ItemIdentifier.get(stack);
        PassiveSinkFinder.Destination destination =
            PassiveSinkFinder.getDestination(stack, itemid, false, serverRouter, List.of());
        if (destination == null) {
            return null;
        }
        extractAndSend(slot, stack, inventory, destination.routerId(), destination.sinkReply());
        return null;
    }

    @Override
    public @Nullable QuicksortResult tickAsync(@Nullable QuicksortSetup setupObject) {
        if (setupObject == null) {
            return null;
        }
        IPipeServiceProvider service = this.service;
        if (service == null || !(service.getRouter() instanceof ServerRouter serverRouter)) {
            return null;
        }
        AsyncRouting.updateRoutingTable(serverRouter);
        ItemIdentifier itemid = ItemIdentifier.get(setupObject.stack());
        // Touches routers and pipes, which only the server thread may do.
        PassiveSinkFinder.Destination destination = LPExecutors.onServerThread(() ->
            PassiveSinkFinder.getDestination(setupObject.stack(), itemid, false, serverRouter, List.of()));
        if (destination == null) {
            return null;
        }
        return new QuicksortResult(setupObject.slot(), itemid, destination.routerId(), destination.sinkReply());
    }

    @Override
    public void completeJob(@Nullable QuicksortResult result) {
        IPipeServiceProvider service = this.service;
        if (result == null || service == null) {
            return;
        }
        List<@Nullable IInventoryUtil> inventories = PipeServiceProviderUtil.availableInventories(service);
        IInventoryUtil inventory = inventories.isEmpty() ? null : inventories.getFirst();
        if (inventory == null || result.slot() >= inventory.getContainerSize()) {
            return;
        }
        ItemStack stack = inventory.getItem(result.slot());
        if (ItemUtil.equalsWithNBT(result.itemid(), stack)) {
            extractAndSend(result.slot(), stack, inventory, result.destRouterId(), result.sinkReply());
        }
    }

    private void extractAndSend(int slot, ItemStack stack, IInventoryUtil inventory, int destRouterId,
        SinkReply sinkReply) {
        IPipeServiceProvider service = this.service;
        if (service == null) {
            return;
        }
        Direction pointedOrientation = service.getPointedOrientation();
        if (pointedOrientation == null) {
            return;
        }
        int toExtract = ItemUtil.getExtractionMax(stack.getCount(), stack.getMaxStackSize(), sinkReply);
        if (toExtract <= 0 || !service.useEnergy(getEnergyPerStack())) {
            return;
        }
        stalled = false;
        stallSlot = slot;
        ItemStack extracted = inventory.removeItem(slot, toExtract);
        if (extracted.isEmpty()) {
            return;
        }
        service.sendStack(extracted, destRouterId, sinkReply, CoreRoutedPipe.ItemSendMode.Fast, pointedOrientation);
        service.spawnParticle(Particles.ORANGE_SPARKLE, 8);
    }

    @Override
    public void runSyncWork() {}

    @Override
    public boolean receivePassive() {
        return false;
    }

    @Override
    public boolean hasGenericInterests() {
        return false;
    }

    @Override
    public boolean interestedInUndamagedID() {
        return false;
    }

    @Override
    public boolean interestedInAttachedInventory() {
        return false;
    }

    private void notifyWorkingSlot(int slot) {
        localSlotWatchers.send(new QuickSortStateMessage(ModuleTarget.of(this), slot));
    }

    public void addWatchingPlayer(Player player) {
        localSlotWatchers.add(player);
        if (player instanceof ServerPlayer serverPlayer) {
            // Where the last tick was, which is the slot that is currently ringed for everyone
            // else watching.
            PacketDistributor.sendToPlayer(serverPlayer,
                new QuickSortStateMessage(ModuleTarget.of(this), Math.max(currentSlot - 1, 0)));
        }
    }

    public void removeWatchingPlayer(Player player) {
        localSlotWatchers.remove(player);
    }
}
