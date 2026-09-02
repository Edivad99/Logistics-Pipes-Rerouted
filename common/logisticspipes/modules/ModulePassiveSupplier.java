package logisticspipes.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.PacketDistributor;

import org.jspecify.annotations.Nullable;

import logisticspipes.gui.hud.modules.HUDSimpleFilterModule;
import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.interfaces.IHUDModuleHandler;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.interfaces.IModuleInventoryReceive;
import logisticspipes.interfaces.IModuleMenuProvider;
import logisticspipes.interfaces.IModuleWatchReciver;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.to_client.module.ModuleInventoryMessage;
import logisticspipes.pipes.PipeLogisticsChassis.ChassiTargetInformation;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.SinkReply.FixedPriority;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.world.inventory.SimpleFilterMenu;
import network.rs485.logisticspipes.module.PipeServiceProviderUtilKt;
import logisticspipes.modules.SimpleFilter;
import network.rs485.logisticspipes.property.ItemIdentifierInventoryProperty;
import network.rs485.logisticspipes.property.Property;

public class ModulePassiveSupplier extends LogisticsModule
		implements SimpleFilter, IClientInformationProvider, IHUDModuleHandler, IModuleWatchReciver,
		IModuleInventoryReceive, ISimpleInventoryEventHandler, IModuleMenuProvider {

	public final ItemIdentifierInventoryProperty filterInventory = new ItemIdentifierInventoryProperty(
			new ItemIdentifierInventory(9, "Requested items", 64), "filterInv");

	private final PlayerCollectionList localModeWatchers = new PlayerCollectionList();
	private final IHUDModuleRenderer HUD = new HUDSimpleFilterModule(this);
	/** Built in {@link #registerPosition}, which runs when the module is installed. */
	private @Nullable SinkReply sinkReply;

	public ModulePassiveSupplier() {
		filterInventory.addListener(this);
	}

	public static String getName() {
		return "passive_supplier";
	}

	@Override
	public String getLPName() {
		return getName();
	}

	@Override
	public List<Property<?>> getProperties() {
		return Collections.singletonList(filterInventory);
	}

	@Override
    public Container getFilterInventory() {
		return filterInventory;
	}

	@Override
	public void registerPosition(ModulePositionType slot, int positionInt) {
		super.registerPosition(slot, positionInt);
		sinkReply = new SinkReply(FixedPriority.PassiveSupplier, 0, true, false, 2, 0,
				new ChassiTargetInformation(getPositionInt()));
	}

	@Override
	public @Nullable SinkReply sinksItem(ItemStack stack, ItemIdentifier item, int bestPriority, int bestCustomPriority,
			boolean allowDefault, boolean includeInTransit, boolean forcePassive) {
		final SinkReply reply = Objects.requireNonNull(sinkReply, "module has not been registered");
		if (bestPriority > reply.fixedPriority.ordinal() || (bestPriority == reply.fixedPriority.ordinal()
				&& bestCustomPriority >= reply.customPriority)) {
			return null;
		}

		final IPipeServiceProvider service = this.service;
		if (service == null) return null;
		final ISlotUpgradeManager upgradeManager = service.getUpgradeManager(slot, positionInt);
		IInventoryUtil targetUtil = PipeServiceProviderUtilKt.availableSneakyInventories(service, upgradeManager)
				.stream().findFirst().orElse(null);
		if (targetUtil == null) {
			return null;
		}

		if (!filterInventory.containsItem(item)) {
			return null;
		}

		int targetCount = filterInventory.itemCount(item);
		int haveCount = targetUtil.itemCount(item);
		if (targetCount <= haveCount) {
			return null;
		}

		if (service.canUseEnergy(2)) {
			return new SinkReply(reply, targetCount - haveCount);
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
	public void startWatching(Player player) {
		localModeWatchers.add(player);
		if (player instanceof ServerPlayer inventoryWatcher) {
			PacketDistributor.sendToPlayer(inventoryWatcher,
					new ModuleInventoryMessage(ModuleTarget.of(this), ItemIdentifierStack.getListFromInventory(filterInventory)));
		}
	}

	@Override
	public void stopWatching(Player player) {
		localModeWatchers.remove(player);
	}

	@Override
	public IHUDModuleRenderer getHUDRenderer() {
		return HUD;
	}

	@Override
	public void handleInvContent(Collection<ItemIdentifierStack> list) {
		filterInventory.handleItemIdentifierList(list);
	}

	@Override
	public void InventoryChanged(Container inventory) {
		MainProxy.runOnServer(getWorld(), () -> () ->
				localModeWatchers.send(
					new ModuleInventoryMessage(ModuleTarget.of(this), ItemIdentifierStack.getListFromInventory(filterInventory)))
		);
	}

	@Override
	public boolean hasGenericInterests() {
		return false;
	}

	@Override
	public void collectSpecificInterests(Collection<ItemIdentifier> itemIdentifiers) {
		itemIdentifiers.addAll(filterInventory.getItemsAndCount().keySet());
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
		return new SimpleFilterMenu(containerId, inventory, target, this);
	}

}
