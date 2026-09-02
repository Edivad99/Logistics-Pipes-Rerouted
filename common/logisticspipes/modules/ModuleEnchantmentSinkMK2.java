package logisticspipes.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.PacketDistributor;

import org.jspecify.annotations.Nullable;

import logisticspipes.gui.hud.modules.HUDSimpleFilterModule;
import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.interfaces.IHUDModuleHandler;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.interfaces.IModuleInventoryReceive;
import logisticspipes.interfaces.IModuleWatchReciver;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.network.abstractguis.ModuleInHandGuiProvider;
import logisticspipes.network.to_client.module.ModuleInventoryMessage;
import logisticspipes.pipes.PipeLogisticsChassis.ChassiTargetInformation;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCType;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.SinkReply.FixedPriority;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.inventory.IItemIdentifierInventory;
import network.rs485.logisticspipes.module.Gui;
import network.rs485.logisticspipes.module.SimpleFilter;
import network.rs485.logisticspipes.property.ItemIdentifierInventoryProperty;
import network.rs485.logisticspipes.property.Property;

@CCType(name = "EnchantmentSink Module MK2")
public class ModuleEnchantmentSinkMK2 extends LogisticsModule
		implements SimpleFilter, IClientInformationProvider, IHUDModuleHandler, IModuleWatchReciver,
		ISimpleInventoryEventHandler, IModuleInventoryReceive, Gui {

	public final ItemIdentifierInventoryProperty filterInventory = new ItemIdentifierInventoryProperty(
			new ItemIdentifierInventory(9, "Requested Enchanted items", 1), "filterInv");

	private final PlayerCollectionList localModeWatchers = new PlayerCollectionList();
	private final IHUDModuleRenderer HUD = new HUDSimpleFilterModule(this);
	/** Built in {@link #registerPosition}, which runs when the module is installed. */
	private @Nullable SinkReply sinkReply;

	public ModuleEnchantmentSinkMK2() {
		filterInventory.addListener(this);
	}

	public static String getName() {
		return "enchantment_sink_mk2";
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
	@CCCommand(description = "Returns the FilterInventory of this Module")
    public IItemIdentifierInventory getFilterInventory() {
		return filterInventory;
	}

	@Override
	public void registerPosition(ModulePositionType slot, int positionInt) {
		super.registerPosition(slot, positionInt);
		sinkReply = new SinkReply(FixedPriority.EnchantmentItemSink, 1, true, false, 1, 0,
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
		if (filterInventory.containsExcludeNBTItem(item.getUndamaged().getIgnoringNBT())) {
			if (stack.isEnchanted()) {
				return reply;
			}
			return null;
		}
		return null;
	}

	@Override
	public void tick() {}

	@Override
	public List<String> getClientInformation() {
		List<String> list = new ArrayList<>();
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
	public void InventoryChanged(Container inventory) {
		MainProxy.runOnServer(getWorld(), () -> () ->
				localModeWatchers.send(
					new ModuleInventoryMessage(ModuleTarget.of(this), ItemIdentifierStack.getListFromInventory(inventory)))
		);
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
	/*
	 * (non-Javadoc)
	 * @see logisticspipes.modules.LogisticsModule#hasGenericInterests()
	 * Only looking for items in filter
	 */
	public boolean hasGenericInterests() {
		return false;
	}

	@Override
	public void collectSpecificInterests(Collection<ItemIdentifier> itemIdentifiers) {
		Map<ItemIdentifier, Integer> mapIC = filterInventory.getItemsAndCount();
		itemIdentifiers.addAll(mapIC.keySet());
		for (ItemIdentifier id : mapIC.keySet()) {
			itemIdentifiers.add(id.getUndamaged());
			itemIdentifiers.add(id.getUndamaged().getIgnoringNBT());
		}
	}

	@Override
	public boolean interestedInAttachedInventory() {
		return false;
	}

	@Override
	public boolean interestedInUndamagedID() {
		return true;
	}

	@Override
	public boolean receivePassive() {
		return true;
	}

	@Override
	public boolean hasEffect() {
		return true;
	}

	@Override
	public ModuleCoordinatesGuiProvider getPipeGuiProvider() {
		return SimpleFilter.getPipeGuiProvider();
	}

	@Override
	public ModuleInHandGuiProvider getInHandGuiProvider() {
		return SimpleFilter.getInHandGuiProvider();
	}

}
