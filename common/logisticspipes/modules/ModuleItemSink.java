package logisticspipes.modules;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.PacketDistributor;

import com.google.common.collect.ImmutableList;
import org.jspecify.annotations.Nullable;

import logisticspipes.gui.hud.modules.HUDItemSink;
import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.interfaces.IHUDModuleHandler;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.interfaces.IModuleInventoryReceive;
import logisticspipes.interfaces.IModuleWatchReciver;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.network.abstractguis.ModuleInHandGuiProvider;
import logisticspipes.network.guis.module.inhand.ItemSinkInHand;
import logisticspipes.network.guis.module.inpipe.ItemSinkSlot;
import logisticspipes.network.packets.hud.HUDStartModuleWatchingPacket;
import logisticspipes.network.packets.hud.HUDStopModuleWatchingPacket;
import logisticspipes.network.packets.module.ModuleInventory;
import logisticspipes.network.to_client.ItemSinkDefaultRouteMessage;
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
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.connection.LPNeighborTileEntityKt;
import network.rs485.logisticspipes.inventory.IItemIdentifierInventory;
import network.rs485.logisticspipes.module.Gui;
import network.rs485.logisticspipes.module.SimpleFilter;
import network.rs485.logisticspipes.property.BitSetProperty;
import network.rs485.logisticspipes.property.BooleanProperty;
import network.rs485.logisticspipes.property.IBitSet;
import network.rs485.logisticspipes.property.ItemIdentifierInventoryProperty;
import network.rs485.logisticspipes.property.Property;
import network.rs485.logisticspipes.util.FuzzyFlag;
import network.rs485.logisticspipes.util.FuzzyUtil;

@CCType(name = "ItemSink Module")
public class ModuleItemSink extends LogisticsModule
	implements SimpleFilter, IClientInformationProvider, IHUDModuleHandler, IModuleWatchReciver,
	ISimpleInventoryEventHandler, IModuleInventoryReceive, Gui {

	public final ItemIdentifierInventoryProperty filterInventory = new ItemIdentifierInventoryProperty(
		new ItemIdentifierInventory(9, "Requested items", 1), "filterInv");
	public final BooleanProperty defaultRoute = new BooleanProperty(false, "defaultdestination");

	public final BitSetProperty fuzzyFlags = new BitSetProperty(
		new BitSet(filterInventory.getContainerSize() * 4), "fuzzyFlags");

	private final List<Property<?>> properties = ImmutableList.<Property<?>>builder()
		.add(filterInventory)
		.add(defaultRoute)
		.add(fuzzyFlags)
		.build();

	private final PlayerCollectionList localModeWatchers = new PlayerCollectionList();
	private final IHUDModuleRenderer HUD = new HUDItemSink(this);
	/** Built in {@link #registerPosition}, which runs when the module is installed. */
	private @Nullable SinkReply sinkReply;
	/** Built in {@link #registerPosition}, which runs when the module is installed. */
	private @Nullable SinkReply sinkReplyDefault;

	public ModuleItemSink() {
		filterInventory.addListener(this);
	}

	public static String getName() {
		return "item_sink";
	}

	@Override
	public String getLPName() {
		return getName();
	}

	@Override
	public List<Property<?>> getProperties() {
		return properties;
	}

	@Override
	@CCCommand(description = "Returns the FilterInventory of this Module")
    public IItemIdentifierInventory getFilterInventory() {
		return filterInventory;
	}

	@CCCommand(description = "Returns true if the module is a default route")
	public boolean isDefaultRoute() {
		return defaultRoute.getValue();
	}

	@CCCommand(description = "Sets the default route status of this module")
	public void setDefaultRoute(Boolean isDefaultRoute) {
		defaultRoute.setValue(isDefaultRoute);
		if (!localModeWatchers.isEmpty()) {
			localModeWatchers.send(new ItemSinkDefaultRouteMessage(ModuleTarget.of(this), isDefaultRoute));
		}
	}

	@Override
	public void registerPosition(ModulePositionType slot, int positionInt) {
		super.registerPosition(slot, positionInt);
		sinkReply = new SinkReply(FixedPriority.ItemSink, 0, true, false, 1, 0,
			new ChassiTargetInformation(getPositionInt()));
		sinkReplyDefault = new SinkReply(FixedPriority.DefaultRoute, 0, true, true, 1, 0,
			new ChassiTargetInformation(getPositionInt()));
	}

	public Stream<ItemIdentifier> getAdjacentInventoriesItems() {
		return Objects.requireNonNull(service)
			.getAvailableAdjacent()
			.inventories()
			.stream()
			.map(LPNeighborTileEntityKt::getInventoryUtil)
			.filter(Objects::nonNull)
			.flatMap(invUtil -> invUtil.getItems().stream())
			.distinct();
	}

	@Override
	public @Nullable SinkReply sinksItem(ItemStack stack, ItemIdentifier item, int bestPriority, int bestCustomPriority,
		boolean allowDefault, boolean includeInTransit, boolean forcePassive) {
		final SinkReply reply = Objects.requireNonNull(sinkReply, "module has not been registered");
		final SinkReply replyDefault = Objects.requireNonNull(sinkReplyDefault, "module has not been registered");
		if (defaultRoute.getValue() && !allowDefault) {
			return null;
		}
		if (bestPriority > reply.fixedPriority.ordinal() || (bestPriority == reply.fixedPriority.ordinal()
			&& bestCustomPriority >= reply.customPriority)) {
			return null;
		}
		final IPipeServiceProvider service = this.service;
		if (service == null) return null;
		if (filterInventory.containsUndamagedItem(item.getUndamaged())) {
			if (service.canUseEnergy(1)) {
				return reply;
			}
			return null;
		}
		final ISlotUpgradeManager upgradeManager = getUpgradeManager();
		if (upgradeManager.isFuzzyUpgrade()) {
			for (Pair<ItemIdentifierStack, Integer> filter : filterInventory.contents()) {
				if (filter == null) {
					continue;
				}
				if (filter.getValue1() == null) {
					continue;
				}
				ItemIdentifier ident1 = item;
				ItemIdentifier ident2 = filter.getValue1().getItem();
				IBitSet slotFlags = getSlotFuzzyFlags(filter.getValue2());
				if (FuzzyUtil.INSTANCE.get(slotFlags, FuzzyFlag.IGNORE_DAMAGE)) {
					ident1 = ident1.getIgnoringData();
					ident2 = ident2.getIgnoringData();
				}
				if (FuzzyUtil.INSTANCE.get(slotFlags, FuzzyFlag.IGNORE_NBT)) {
					ident1 = ident1.getIgnoringNBT();
					ident2 = ident2.getIgnoringNBT();
				}
				if (ident1.equals(ident2)) {
					if (service.canUseEnergy(5)) {
						return reply;
					}
					return null;
				}
			}
		}
		if (defaultRoute.getValue()) {
			if (bestPriority > replyDefault.fixedPriority.ordinal() || (
				bestPriority == replyDefault.fixedPriority.ordinal()
					&& bestCustomPriority >= replyDefault.customPriority)) {
				return null;
			}
			if (service.canUseEnergy(1)) {
				return replyDefault;
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
		list.add("Default: " + (isDefaultRoute() ? "Yes" : "No"));
		list.add("<inventory>");
		list.add("<that>" + filterInventory.getTagKey());
		return list;
	}

	@Override
	public void startHUDWatching() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStartModuleWatchingPacket.class).setModulePos(this));
	}

	@Override
	public void stopHUDWatching() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStopModuleWatchingPacket.class).setModulePos(this));
	}

	@Override
	public void startWatching(Player player) {
		localModeWatchers.add(player);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(ModuleInventory.class)
			.setIdentList(ItemIdentifierStack.getListFromInventory(filterInventory)).setModulePos(this), player);
		if (player instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer,
				new ItemSinkDefaultRouteMessage(ModuleTarget.of(this), defaultRoute.getValue()));
		}
	}

	@Override
	public void stopWatching(Player player) {
		localModeWatchers.remove(player);
	}

	@Override
	public void InventoryChanged(Container inventory) {
		MainProxy.runOnServer(getWorld(), () -> () ->
			MainProxy.sendToPlayerList(
				PacketHandler.getPacket(ModuleInventory.class)
					.setIdentList(ItemIdentifierStack.getListFromInventory(inventory))
					.setModulePos(this),
				localModeWatchers
			)
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
	public boolean hasGenericInterests() {
		return defaultRoute.getValue();
	}

	@Override
	public void collectSpecificInterests(Collection<ItemIdentifier> itemIdentifiers) {
		if (defaultRoute.getValue()) {
			return;
		}
		Map<ItemIdentifier, Integer> mapIC = filterInventory.getItemsAndCount();
		itemIdentifiers.addAll(mapIC.keySet());
		mapIC.keySet().stream().map(ItemIdentifier::getUndamaged).forEach(itemIdentifiers::add);
		if (getUpgradeManager().isFuzzyUpgrade()) {
			for (Pair<ItemIdentifierStack, Integer> stack : filterInventory.contents()) {
				if (stack.getValue1() == null) {
					continue;
				}
				ItemIdentifier ident = stack.getValue1().getItem();
				IBitSet slotFlags = getSlotFuzzyFlags(stack.getValue2());
				if (FuzzyUtil.INSTANCE.get(slotFlags, FuzzyFlag.IGNORE_DAMAGE)) {
					itemIdentifiers.add(ident.getIgnoringData());
				}
				if (FuzzyUtil.INSTANCE.get(slotFlags, FuzzyFlag.IGNORE_NBT)) {
					itemIdentifiers.add(ident.getIgnoringNBT());
				}
				if (FuzzyUtil.INSTANCE.get(slotFlags, FuzzyFlag.IGNORE_DAMAGE) && FuzzyUtil.INSTANCE.get(slotFlags, FuzzyFlag.IGNORE_NBT)) {
					itemIdentifiers.add(ident.getIgnoringData().getIgnoringNBT());
				}
			}
		}
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
		return true;
	}

	public void setFuzzyFlags(BitSet fuzzyFlags) {
		this.fuzzyFlags.replaceWith(fuzzyFlags);
	}

	@Override
	public ModuleCoordinatesGuiProvider getPipeGuiProvider() {
		return NewGuiHandler.getGui(ItemSinkSlot.class).setDefaultRoute(defaultRoute.getValue())
			.setFuzzyFlags(fuzzyFlags.copyValue()).setHasFuzzyUpgrade(getUpgradeManager().isFuzzyUpgrade());
	}

	@Override
	public ModuleInHandGuiProvider getInHandGuiProvider() {
		return NewGuiHandler.getGui(ItemSinkInHand.class);
	}

	public IBitSet getSlotFuzzyFlags(int slotId) {
		final int startBit = slotId * 4;
		return fuzzyFlags.get(startBit, startBit + 3);
	}

}

