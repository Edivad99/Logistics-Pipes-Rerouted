package logisticspipes.routing.item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.request.order.IDistanceTracker;
import logisticspipes.routing.item.IRoutedItem.TransportMode;
import logisticspipes.ticks.LPTickHandler;
import logisticspipes.util.ItemStackLoader;
import logisticspipes.utils.item.ItemIdentifierStack;

public class ItemRoutingInformation {

    public static class DelayComparator implements Comparator<ItemRoutingInformation> {

        @Override
        public int compare(ItemRoutingInformation o1, ItemRoutingInformation o2) {
            // cast will never overflow because the delta is in 1/20ths of a second.
            return (int) (o2.getTimeOut() - o1.getTimeOut());
        }
    }

    @Override
    public ItemRoutingInformation clone() {
        ItemRoutingInformation that = new ItemRoutingInformation();
        that.destinationint = destinationint;
        that.destinationUUID = destinationUUID;
        that.arrived = arrived;
        that.bufferCounter = bufferCounter;
        that.doNotBuffer = doNotBuffer;
        that.transportMode = transportMode;
        that.jamlist = new ArrayList<>(jamlist);
        that.tracker = tracker;
        that.targetInfo = targetInfo;
        that.item = new ItemIdentifierStack(getItem());
        return that;
    }

    public int destinationint = -1;
    @Nullable
    public UUID destinationUUID;
    public boolean arrived;
    public int bufferCounter = 0;
    public boolean doNotBuffer;
    public TransportMode transportMode = TransportMode.Unknown;
    public List<Integer> jamlist = new ArrayList<>();
    @Nullable
    public IDistanceTracker tracker = null;
    @Nullable
    public IAdditionalTargetInformation targetInfo;

    private long delay = 640 + LPTickHandler.getServerTick();

    @Getter
    @Setter
    private ItemIdentifierStack item;

    public void deserialize(ValueInput input) {
        destinationUUID = input.read("destinationUUID", UUIDUtil.LENIENT_CODEC).orElse(null);
        arrived = input.getBooleanOr("arrived", false);
        bufferCounter = input.getIntOr("bufferCounter", 0);
        transportMode = TransportMode.values()[input.getIntOr("transportMode", 0)];
        setItem(ItemIdentifierStack.getFromStack(ItemStackLoader.loadItemStack(input, "Item")));
    }

    public void serialize(ValueOutput output) {
        output.storeNullable("destinationUUID", UUIDUtil.CODEC, destinationUUID);
        output.putBoolean("arrived", arrived);
        output.putInt("bufferCounter", bufferCounter);
        output.putInt("transportMode", transportMode.ordinal());
        ItemStackLoader.saveItemStack(output, "Item", getItem().makeNormalStack());
    }

    // the global LP tick in which getTickToTimeOut returns 0.
    public long getTimeOut() {
        return delay;
    }

    // how many ticks until this times out
    public long getTickToTimeOut() {
        return delay - LPTickHandler.getServerTick();
    }

    public void resetDelay() {
        delay = 640 + LPTickHandler.getServerTick();
        if (tracker != null) {
            tracker.setDelay(delay);
        }
    }

    public void setItemTimedout() {
        delay = LPTickHandler.getServerTick() - 1;
        if (tracker != null) {
            tracker.setDelay(delay);
        }
    }

    @Override
    public String toString() {
        return String.format("(%s, %d, %s, %s, %s, %d, %s)", item, destinationint, destinationUUID, transportMode,
            jamlist, delay, tracker);
    }

    public void storeToNBT(ValueOutput output) {
        UUID uuid = UUID.randomUUID();
        output.putString("StoreUUID", uuid.toString());
        this.serialize(output);
        storeMap.put(uuid, this);
    }

    public static ItemRoutingInformation restoreFromNBT(ValueInput input) {
        Optional<UUID> stored = input.getString("StoreUUID").map(UUID::fromString);
        if (stored.isPresent() && storeMap.containsKey(stored.get())) {
            return storeMap.remove(stored.get());
        }
        ItemRoutingInformation info = new ItemRoutingInformation();
        info.deserialize(input);
        return info;
    }

    private static final Map<UUID, ItemRoutingInformation> storeMap = new HashMap<>();
}
