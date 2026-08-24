package logisticspipes.modules;

import java.util.Collections;
import java.util.List;

import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.pipes.PipeLogisticsChassis.ChassiTargetInformation;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.SinkReply.FixedPriority;
import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.module.PipeServiceProviderUtilKt;
import network.rs485.logisticspipes.property.Property;

public class ModulePolymorphicItemSink extends LogisticsModule {

	private SinkReply sinkReply;

	public static String getName() {
		return "item_sink_polymorphic";
	}

	@Override
	public String getLPName() {
		return getName();
	}

	@Override
	public List<Property<?>> getProperties() {
		return Collections.emptyList();
	}

	@Override
	public void registerPosition(ModulePositionType slot, int positionInt) {
		super.registerPosition(slot, positionInt);
		sinkReply = new SinkReply(FixedPriority.ItemSink,
				0,
				true,
				false,
				3,
				0,
				new ChassiTargetInformation(getPositionInt()));
	}

	@Override
	public @Nullable SinkReply sinksItem(ItemStack stack, ItemIdentifier item, int bestPriority, int bestCustomPriority,
			boolean allowDefault, boolean includeInTransit, boolean forcePassive) {
		if (bestPriority > sinkReply.fixedPriority.ordinal() || (bestPriority == sinkReply.fixedPriority.ordinal()
				&& bestCustomPriority >= sinkReply.customPriority)) {
			return null;
		}
		final IPipeServiceProvider service = this.service;
		if (service == null) return null;
		final ISlotUpgradeManager upgradeManager = service.getUpgradeManager(slot, positionInt);
		IInventoryUtil targetInventory = PipeServiceProviderUtilKt.availableSneakyInventories(service, upgradeManager)
				.stream().findFirst().orElse(null);
		if (targetInventory == null) {
			return null;
		}

		if (!targetInventory.containsUndamagedItem(item.getUndamaged())) {
			return null;
		}

		if (service.canUseEnergy(3)) {
			return sinkReply;
		}
		return null;
	}

	@Override
	public void tick() {}

	@Override
	public boolean hasGenericInterests() {
		return false;
	}

	@Override
	public boolean interestedInAttachedInventory() {
		return true; // by definition :)
	}

	@Override
	public boolean interestedInUndamagedID() {
		return true;
	}

	@Override
	public boolean receivePassive() {
		return true;
	}

}
