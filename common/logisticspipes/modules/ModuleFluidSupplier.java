package logisticspipes.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.network.abstractguis.ModuleInHandGuiProvider;
import logisticspipes.network.guis.module.inpipe.FluidSupplierSlot;
import logisticspipes.particle.Particles;
import logisticspipes.pipes.PipeLogisticsChassis.ChassiTargetInformation;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.SinkReply.FixedPriority;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import network.rs485.logisticspipes.module.Gui;
import network.rs485.logisticspipes.property.ItemIdentifierInventoryProperty;
import network.rs485.logisticspipes.property.Property;

public class ModuleFluidSupplier extends LogisticsModule implements IClientInformationProvider, Gui {

	private final ItemIdentifierInventoryProperty filterInventory = new ItemIdentifierInventoryProperty(
			new ItemIdentifierInventory(9, "Requested liquids", 1), "filterInv");

	private SinkReply sinkReply;

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
		if (bestPriority > sinkReply.fixedPriority.ordinal()
				|| (bestPriority == sinkReply.fixedPriority.ordinal()
				&& bestCustomPriority >= sinkReply.customPriority)) {
			return null;
		}
		final IPipeServiceProvider service = this.service;
		if (service == null) return null;
		if (filterInventory.containsItem(item)) {
			service.spawnParticle(Particles.VIOLET_SPARKLE, 2);
			return sinkReply;
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
	public ModuleCoordinatesGuiProvider getPipeGuiProvider() {
		return NewGuiHandler.getGui(FluidSupplierSlot.class);
	}

	@Override
	public ModuleInHandGuiProvider getInHandGuiProvider() {
		throw new UnsupportedOperationException("Fluid Supplier GUI cannot be opened in hand");
	}

}
