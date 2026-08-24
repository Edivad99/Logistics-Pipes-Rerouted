package logisticspipes.modules;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.SinkReply.FixedPriority;
import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.connection.LPNeighborTileEntityKt;
import network.rs485.logisticspipes.property.Property;

public class ModuleSatellite extends LogisticsModule {

	private final SinkReply sinkReply = new SinkReply(FixedPriority.ItemSink, 0, true, false, 1, 0, null);

	@Override
	public String getLPName() {
		throw new RuntimeException("Cannot get LP name for " + this);
	}

	@Override
	public List<Property<?>> getProperties() {
		return Collections.emptyList();
	}

	@Override
	public @Nullable SinkReply sinksItem(ItemStack stack, ItemIdentifier item, int bestPriority, int bestCustomPriority,
			boolean allowDefault, boolean includeInTransit, boolean forcePassive) {
		if (bestPriority > sinkReply.fixedPriority.ordinal() || (bestPriority == sinkReply.fixedPriority.ordinal()
				&& bestCustomPriority >= sinkReply.customPriority)) {
			return null;
		}
		final int itemCount = spaceFor(stack, item, includeInTransit);
		if (itemCount > 0) {
			return new SinkReply(sinkReply, itemCount);
		} else {
			return null;
		}
	}

	private int spaceFor(ItemStack stack, ItemIdentifier item, boolean includeInTransit) {
		final IPipeServiceProvider service = Objects.requireNonNull(this.service);
		int count = service.getAvailableAdjacent().inventories().stream()
				.map(neighbor -> LPNeighborTileEntityKt.sneakyInsertion(neighbor).from(getUpgradeManager()))
				.map(LPNeighborTileEntityKt::getInventoryUtil)
				.filter(Objects::nonNull)
				.map(util -> util.roomForItem(stack))
				.reduce(Integer::sum).orElse(0);
		if (includeInTransit) {
			count -= service.countOnRoute(item);
		}
		return count;
	}

	@Override
	public void tick() {}

	@Override
	public boolean hasGenericInterests() {
		return false;
	}

	@Override
	public boolean interestedInAttachedInventory() {
		return false;
		// when we are default we are interested in everything anyway, otherwise we're only interested in our filter.
	}

	@Override
	public boolean interestedInUndamagedID() {
		return false;
	}

	@Override
	public boolean receivePassive() {
		return false;
	}

}
