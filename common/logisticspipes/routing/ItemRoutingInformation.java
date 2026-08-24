package logisticspipes.routing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.logisticspipes.IRoutedItem.TransportMode;
import logisticspipes.proxy.MainProxy;
import logisticspipes.routing.order.IDistanceTracker;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.util.items.ItemStackLoader;

public class ItemRoutingInformation {

	public static class DelayComparator implements Comparator<ItemRoutingInformation> {

		@Override
		public int compare(ItemRoutingInformation o1, ItemRoutingInformation o2) {
			return (int) (o2.getTimeOut() - o1.getTimeOut()); // cast will never overflow because the delta is in 1/20ths of a second.
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

	private long delay = 640 + MainProxy.getGlobalTick();

	@Getter
	@Setter
	private ItemIdentifierStack item;

	public void deserialize(ValueInput input) {
		input.getString("destinationUUID").ifPresent(raw -> destinationUUID = UUID.fromString(raw));
		arrived = input.getBooleanOr("arrived", false);
		bufferCounter = input.getIntOr("bufferCounter", 0);
		transportMode = TransportMode.values()[input.getIntOr("transportMode", 0)];
		setItem(ItemIdentifierStack.getFromStack(ItemStackLoader.loadItemStack(input, "Item")));
	}

	public void serialize(ValueOutput output) {
		if (destinationUUID != null) {
			output.putString("destinationUUID", destinationUUID.toString());
		}
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
		return delay - MainProxy.getGlobalTick();
	}

	public void resetDelay() {
		delay = 640 + MainProxy.getGlobalTick();
		if (tracker != null) {
			tracker.setDelay(delay);
		}
	}

	public void setItemTimedout() {
		delay = MainProxy.getGlobalTick() - 1;
		if (tracker != null) {
			tracker.setDelay(delay);
		}
	}

	@Override
	public String toString() {
		return String.format("(%s, %d, %s, %s, %s, %d, %s)", item, destinationint, destinationUUID, transportMode, jamlist, delay, tracker);
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
