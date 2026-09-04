package logisticspipes.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.interfaces.IModuleMenuProvider;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.network.ModuleTarget;
import logisticspipes.particle.Particles;
import logisticspipes.pipes.PipeLogisticsChassis.ChassiTargetInformation;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.SinkReply.FixedPriority;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.world.inventory.LPMenuTypes;
import logisticspipes.world.inventory.SimpleFilterMenu;
import network.rs485.logisticspipes.property.ItemIdentifierInventoryProperty;
import network.rs485.logisticspipes.property.Property;

public class ModuleFluidSupplier extends LogisticsModule
		implements IClientInformationProvider, SimpleFilter, IModuleMenuProvider {

	private final ItemIdentifierInventoryProperty filterInventory = new ItemIdentifierInventoryProperty(
			new ItemIdentifierInventory(9, "Requested liquids", 1), "filterInv");

	/** Built in {@link #registerPosition}, which runs when the module is installed. */
	private @Nullable SinkReply sinkReply;

	@Override
	public String getLPName() {
		throw new RuntimeException("Cannot get LP name for " + this);
	}

	@Override
	public List<Property<?>> getProperties() {
		return Collections.singletonList(filterInventory);
	}

	public Container getFilterInventory() {
		return filterInventory;
	}

	@Override
	public void registerPosition(ModulePositionType slot, int positionInt) {
		super.registerPosition(slot, positionInt);
		sinkReply = new SinkReply(FixedPriority.ItemSink,
				0,
				true,
				false,
				0,
				0,
				new ChassiTargetInformation(getPositionInt()));
	}

	@Override
	public @Nullable SinkReply sinksItem(ItemStack stack, ItemIdentifier item, int bestPriority, int bestCustomPriority,
			boolean allowDefault, boolean includeInTransit, boolean forcePassive) {
		final SinkReply reply = Objects.requireNonNull(sinkReply, "module has not been registered");
		if (bestPriority > reply.fixedPriority.ordinal()
				|| (bestPriority == reply.fixedPriority.ordinal()
				&& bestCustomPriority >= reply.customPriority)) {
			return null;
		}
		final IPipeServiceProvider service = this.service;
		if (service == null) return null;
		if (filterInventory.containsItem(item)) {
			service.spawnParticle(Particles.VIOLET_SPARKLE, 2);
			return reply;
		}
		return null;
	}

	@Override
	public void tick() {}

	@Override
	public List<String> getClientInformation() {
		List<String> list = new ArrayList<>();
		list.add("Supplied: ");
		list.add("<inventory>");
		list.add("<that>" + filterInventory.getTagKey());
		return list;
	}

	@Override
	public boolean hasGenericInterests() {
		return true;
	}

	@Override
	public boolean interestedInAttachedInventory() {
		return false;
	}

	@Override
	public boolean interestedInUndamagedID() {
		return false;
	}

	@Override
	public boolean receivePassive() {
		return true;
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, ModuleTarget target) {
		return new SimpleFilterMenu(LPMenuTypes.FLUID_SUPPLIER_MODULE.get(), containerId, inventory, target, this);
	}

}
