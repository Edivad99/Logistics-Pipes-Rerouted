package logisticspipes.pipes.signs;

import java.util.BitSet;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

import logisticspipes.network.TargetLookup;
import logisticspipes.network.to_client.pipe.ItemAmountSignMessage;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.ServerRouter;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;
import logisticspipes.world.inventory.ItemAmountSignMenu;

/**
 * Shows how many of one item the network can provide, counted from the pipe it hangs on.
 */
public final class ItemAmountPipeSign implements IPipeSign, ISimpleInventoryEventHandler {

    public final ItemIdentifierInventory itemTypeInv = new ItemIdentifierInventory(1, "", 1);
    public int amount = 100;
    private final CoreRoutedPipe pipe;
    private final Direction dir;
    /**
     * Set when a neighbouring sign already counted for this one, so the next own count is skipped.
     */
    private boolean hasUpdated = false;

    ItemAmountPipeSign(CoreRoutedPipe pipe, Direction dir) {
        this.pipe = pipe;
        this.dir = dir;
        itemTypeInv.addListener(this);
    }

    @Override
    public PipeSignType type() {
        return PipeSignType.ITEM_AMOUNT;
    }

    /**
     * The item counted, or null when none is set.
     */
    public @Nullable ItemIdentifierStack getItem() {
        return itemTypeInv.getIDStackInSlot(0);
    }

    @Override
    public void activate(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, _) -> new ItemAmountSignMenu(containerId, inventory, pipe, dir),
                    Component.empty()),
                buffer -> {
                    buffer.writeBlockPos(pipe.getPos());
                    Direction.STREAM_CODEC.encode(buffer, dir);
                });
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        itemTypeInv.deserialize(input);
    }

    @Override
    public void serialize(ValueOutput output) {
        itemTypeInv.serialize(output);
    }

    @Override
    public CustomPacketPayload getPacket() {
        return new ItemAmountSignMessage(pipe.getPos(), dir, amount, Optional.ofNullable(getItem()));
    }

    @Override
    public void updateServerSide() {
        if (!pipe.isNthTick(20)) {
            return;
        }
        if (hasUpdated) {
            hasUpdated = false;
            return;
        }
        if (getItem() == null) {
            setAmount(0);
            return;
        }
        Map<ItemIdentifier, Integer> availableItems = SimpleServiceLocator.logisticsManager.getAvailableItems(
            pipe.getRouter().getIRoutersByCost());
        spread(availableItems, new BitSet(ServerRouter.getBiggestSimpleID()));
        setAmount(countIn(availableItems));
    }

    /**
     * Hands the count just made to the amount signs on the pipes close by, so a wall of them is
     * updated at once instead of each one asking the network again.
     */
    private void spread(Map<ItemIdentifier, Integer> availableItems, BitSet visited) {
        IRouter router = pipe.getRouter();
        if (visited.get(router.getSimpleID())) {
            return;
        }
        visited.set(router.getSimpleID());
        for (ExitRoute exit : router.getIRoutersByCost()) {
            if (exit.distanceToDestination > 2) {
                break; // Only as far as one wall of signs reaches.
            }
            if (!exit.filters.isEmpty() || visited.get(exit.destination.getSimpleID())) {
                continue;
            }
            if (exit.connectionDetails.contains(PipeRoutingConnectionType.CAN_REQUEST_FROM)
                && exit.connectionDetails.contains(PipeRoutingConnectionType.CAN_ROUTE_TO)) {
                CoreRoutedPipe cachedPipe = exit.destination.getCachedPipe();
                if (cachedPipe != null) {
                    for (Pair<Direction, IPipeSign> signPair : cachedPipe.getPipeSigns()) {
                        if (signPair.getValue2() instanceof ItemAmountPipeSign sign) {
                            sign.updateStats(availableItems, visited);
                        }
                    }
                }
            }
        }
    }

    private void updateStats(Map<ItemIdentifier, Integer> availableItems, BitSet visited) {
        hasUpdated = true;
        setAmount(countIn(availableItems));
        spread(availableItems, visited);
    }

    private int countIn(Map<ItemIdentifier, Integer> availableItems) {
        ItemIdentifierStack item = getItem();
        return item == null ? 0 : availableItems.getOrDefault(item.getItem(), 0);
    }

    private void setAmount(int newAmount) {
        if (newAmount != amount) {
            amount = newAmount;
            sendUpdatePacket();
        }
    }

    @Override
    public void InventoryChanged(Container inventory) {
        if (inventory == itemTypeInv) {
            sendUpdatePacket();
        }
    }

    private void sendUpdatePacket() {
        if (!pipe.getLevel().isClientSide()) {
            TargetLookup.sendToChunkWatchers(pipe.getContainer(), getPacket());
        }
    }
}
