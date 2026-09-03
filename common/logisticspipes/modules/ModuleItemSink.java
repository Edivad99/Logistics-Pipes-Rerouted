package logisticspipes.modules;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueOutput;

import net.neoforged.neoforge.network.PacketDistributor;

import com.google.common.collect.ImmutableList;
import org.jspecify.annotations.Nullable;

import logisticspipes.gui.hud.modules.HUDItemSink;
import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.interfaces.IHUDModuleHandler;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.interfaces.IModuleInventoryReceive;
import logisticspipes.interfaces.IModuleMenuProvider;
import logisticspipes.interfaces.IModuleWatchReciver;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.to_client.module.ItemSinkDefaultRouteMessage;
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
import logisticspipes.utils.tuples.Pair;
import logisticspipes.world.inventory.LPMenuTypes;
import network.rs485.logisticspipes.connection.LPNeighborTileEntityKt;
import network.rs485.logisticspipes.inventory.IItemIdentifierInventory;
import network.rs485.logisticspipes.inventory.container.ItemSinkContainer;
import logisticspipes.modules.SimpleFilter;
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
	ISimpleInventoryEventHandler, IModuleInventoryReceive, IModuleMenuProvider {

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
	public void startWatching(Player player) {
		localModeWatchers.add(player);
		if (player instanceof ServerPlayer inventoryWatcher) {
			PacketDistributor.sendToPlayer(inventoryWatcher,
					new ModuleInventoryMessage(ModuleTarget.of(this), ItemIdentifierStack.getListFromInventory(filterInventory)));
		}
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
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, ModuleTarget target) {
		return new ItemSinkContainer(LPMenuTypes.ITEM_SINK.get(), containerId, inventory, this, target,
			hasFuzzyUpgradeForScreen(), target.heldStack(inventory));
	}

	@Override
	public void writeMenuData(RegistryFriendlyByteBuf buffer) {
		buffer.writeBoolean(hasFuzzyUpgradeForScreen());
		TagValueOutput moduleOutput = TagValueOutput.createWithContext(
			ProblemReporter.DISCARDING, buffer.registryAccess());
		serialize(moduleOutput);
		buffer.writeNbt(moduleOutput.buildResult());
	}

	/**
	 * Whether the filter slots take fuzzy flags.
	 *
	 * <p>Upgrades belong to the pipe the module sits in; one held in hand has no pipe, so asking
	 * for its upgrade manager would fail rather than answer no.
	 */
	private boolean hasFuzzyUpgradeForScreen() {
		return getSlot() != ModulePositionType.IN_HAND && getUpgradeManager().isFuzzyUpgrade();
	}

	public IBitSet getSlotFuzzyFlags(int slotId) {
		final int startBit = slotId * 4;
		return fuzzyFlags.get(startBit, startBit + 3);
	}

}

