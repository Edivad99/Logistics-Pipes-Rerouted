package logisticspipes.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.PacketDistributor;

import com.google.common.collect.ImmutableList;

import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.interfaces.IHUDModuleHandler;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.interfaces.IModuleInventoryReceive;
import logisticspipes.interfaces.IModuleWatchReciver;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.interfaces.routing.IRequireReliableTransport;
import logisticspipes.interfaces.routing.ITargetSlotInformation;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.network.abstractguis.ModuleInHandGuiProvider;
import logisticspipes.network.guis.module.inhand.ActiveSupplierInHand;
import logisticspipes.network.guis.module.inpipe.ActiveSupplierSlot;
import logisticspipes.network.to_client.module.ModuleInventoryMessage;
import logisticspipes.particle.Particles;
import logisticspipes.pipes.PipeLogisticsChassis.ChassiTargetInformation;
import logisticspipes.pipes.basic.debug.StatusEntry;
import logisticspipes.proxy.MainProxy;
import logisticspipes.request.RequestTree;
import logisticspipes.routing.IRouter;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.connection.AdjacentUtilKt;
import network.rs485.logisticspipes.module.Gui;
import network.rs485.logisticspipes.property.BooleanProperty;
import network.rs485.logisticspipes.property.EnumProperty;
import network.rs485.logisticspipes.property.IntListProperty;
import network.rs485.logisticspipes.property.ItemIdentifierInventoryProperty;
import network.rs485.logisticspipes.property.Property;

public class ModuleActiveSupplier extends LogisticsModule
		implements IRequestItems, IRequireReliableTransport, IClientInformationProvider, IHUDModuleHandler,
		IModuleWatchReciver, IModuleInventoryReceive, ISimpleInventoryEventHandler, Gui {

	public static final int SUPPLIER_SLOTS = 9;

	private final PlayerCollectionList localModeWatchers = new PlayerCollectionList();
	private final HashMap<ItemIdentifier, Integer> requestedItems = new HashMap<>();

	// properties for the pattern upgrade
	public final IntListProperty slotAssignmentPattern = new IntListProperty("slotpattern");
	public final EnumProperty<PatternMode> patternMode =
			new EnumProperty<>(PatternMode.Bulk50, "patternmode", PatternMode.values());

	// properties for the regular configuration
	public final ItemIdentifierInventoryProperty inventory =
			new ItemIdentifierInventoryProperty(new ItemIdentifierInventory(SUPPLIER_SLOTS, "", 127), "supplierInv");
	public final EnumProperty<SupplyMode> requestMode =
			new EnumProperty<>(SupplyMode.Bulk50, "requestmode", SupplyMode.values());
	public final BooleanProperty isLimited = new BooleanProperty(true, "limited");

	private final List<Property<?>> properties = ImmutableList.<Property<?>>builder()
			.add(slotAssignmentPattern)
			.add(patternMode)
			.add(inventory)
			.add(requestMode)
			.add(isLimited)
			.build();

	private boolean lastRequestFailed = false;

	public ModuleActiveSupplier() {
		inventory.addListener(this);
		slotAssignmentPattern.ensureSize(SUPPLIER_SLOTS);
	}

	public static String getName() {
		return "active_supplier";
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
	public List<String> getClientInformation() {
		List<String> list = new ArrayList<>();
		list.add("Supplied: ");
		list.add("<inventory>");
		list.add("<that>" + inventory.getTagKey());
		return list;
	}



	@Override
	public void startWatching(Player player) {
		localModeWatchers.add(player);
		if (player instanceof ServerPlayer inventoryWatcher) {
			PacketDistributor.sendToPlayer(inventoryWatcher, new ModuleInventoryMessage(
					ModuleTarget.of(this), ItemIdentifierStack.getListFromInventory(inventory)));
		}
	}

	@Override
	public void stopWatching(Player player) {
		localModeWatchers.remove(player);
	}

	@Override
	public IHUDModuleRenderer getHUDRenderer() {
		return null;
		//return HUD;
	}

	@Override
	public void handleInvContent(Collection<ItemIdentifierStack> list) {
		inventory.handleItemIdentifierList(list);
	}

	@Override
	public void InventoryChanged(Container inventory) {
		if (MainProxy.isServer(getWorld())) {
			localModeWatchers.send(
					new ModuleInventoryMessage(ModuleTarget.of(this), ItemIdentifierStack.getListFromInventory(inventory)));
		}
	}

	@Override
	public boolean hasGenericInterests() {
		return false;
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

	/* TRIGGER INTERFACE */
	public boolean isRequestFailed() {
		return lastRequestFailed;
	}

	public void setRequestFailed(boolean value) {
		lastRequestFailed = value;
	}

	@Override
	public void tick() {
		final IPipeServiceProvider service = Objects.requireNonNull(this.service);
		if (!service.isNthTick(100)) {
			return;
		}

		requestedItems.values().stream().filter(amount -> amount > 0)
				.forEach(amount -> service.spawnParticle(Particles.VIOLET_SPARKLE, 2));

		AdjacentUtilKt.sneakyInventoryUtils(service.getAvailableAdjacent(), getUpgradeManager()).stream()
				.filter(invUtil -> invUtil != null && invUtil.getContainerSize() > 0)
				.forEach(invUtil -> {
					if (getUpgradeManager().hasPatternUpgrade()) {
						createPatternRequest(invUtil);
					} else {
						createSupplyRequest(invUtil);
					}
				});
	}

	private void createPatternRequest(IInventoryUtil invUtil) {
		final IPipeServiceProvider service = Objects.requireNonNull(this.service);
		service.getDebug().log("Supplier: Start calculating pattern request");
		setRequestFailed(false);

		for (int i = 0; i < SUPPLIER_SLOTS; i++) {
			ItemIdentifierStack needed = inventory.getIDStackInSlot(i);
			if (needed == null) {
				continue;
			}
			final Integer slotAssignedTo = slotAssignmentPattern.get(i);
			if (invUtil.getContainerSize() <= slotAssignedTo) {
				continue;
			}
			ItemStack stack = invUtil.getItem(slotAssignedTo);
			ItemIdentifierStack have = null;
			if (!stack.isEmpty()) {
				have = ItemIdentifierStack.getFromStack(stack);
			}
			int haveCount = 0;
			if (have != null) {
				if (!have.getItem().equals(needed.getItem())) {
					service.getDebug().log("Supplier: Slot for " + i + ", " + needed + " already taken by " + have);
					setRequestFailed(true);
					continue;
				}
				haveCount = have.getStackSize();
			}
			if ((patternMode.getValue() == PatternMode.Bulk50 && haveCount > needed.getStackSize() / 2) || (
					patternMode.getValue() == PatternMode.Bulk100 && haveCount > 0)) {
				continue;
			}

			Integer requestedCount = requestedItems.get(needed.getItem());
			if (requestedCount != null) {
				haveCount += requestedCount;
			}

			int neededCount = needed.getStackSize() - haveCount;
			if (neededCount < 1) {
				continue;
			}

			ItemIdentifierStack toRequest = new ItemIdentifierStack(needed.getItem(), neededCount);

			service.getDebug().log("Supplier: Missing for slot " + i + ": " + toRequest);

			if (!service.useEnergy(10)) {
				break;
			}

			boolean success = false;

			IAdditionalTargetInformation targetInformation = new PatternSupplierTargetInformation(slotAssignedTo,
					needed.getStackSize());

			if (patternMode.getValue() != PatternMode.Full) {
				service.getDebug().log("Supplier: Requesting partial: " + toRequest);
				neededCount = RequestTree.requestPartial(toRequest, this, targetInformation);
				service.getDebug().log("Supplier: Requested: " + toRequest.getItem().makeStack(neededCount));
				if (neededCount > 0) {
					success = true;
				}
			} else {
				service.getDebug().log("Supplier: Requesting: " + toRequest);
				success = RequestTree.request(toRequest, this, null, targetInformation);
				if (success) {
					service.getDebug().log("Supplier: Request success");
				} else {
					service.getDebug().log("Supplier: Request failed");
				}
			}

			if (success) {
				Integer currentRequest = requestedItems.get(toRequest.getItem());
				if (currentRequest == null) {
					requestedItems.put(toRequest.getItem(), neededCount);
				} else {
					requestedItems.put(toRequest.getItem(), currentRequest + neededCount);
				}
			} else {
				setRequestFailed(true);
			}
		}
	}

	private void createSupplyRequest(IInventoryUtil invUtil) {
		final IPipeServiceProvider service = Objects.requireNonNull(this.service);
		service.getDebug().log("Supplier: Start calculating supply request");
		//How many do I want?
		HashMap<ItemIdentifier, Integer> needed = new HashMap<>(inventory.getItemsAndCount());
		service.getDebug().log("Supplier: Needed: " + needed);

		//How many do I have?
		Map<ItemIdentifier, Integer> have = invUtil.getItemsAndCount();
		service.getDebug().log("Supplier: Have:   " + have);

		//How many do I have?
		HashMap<ItemIdentifier, Integer> haveUndamaged = new HashMap<>();
		for (Entry<ItemIdentifier, Integer> item : have.entrySet()) {
			haveUndamaged.merge(item.getKey().getUndamaged(), item.getValue(), Integer::sum);
		}

		//Reduce what I have and what have been requested already
		for (Entry<ItemIdentifier, Integer> item : needed.entrySet()) {
			int haveCount = haveUndamaged.getOrDefault(item.getKey().getUndamaged(), 0);
			int spaceAvailable = invUtil.roomForItem(item.getKey().makeNormalStack(Integer.MAX_VALUE));
			if (requestMode.getValue() == SupplyMode.Infinite) {
				Integer requestedCount = requestedItems.get(item.getKey());
				if (requestedCount != null) {
					spaceAvailable -= requestedCount;
				}
				item.setValue(Math.min(item.getKey().getMaxStackSize(), Math.max(0, spaceAvailable)));
				continue;
			}
			if (spaceAvailable < 1
					|| (requestMode.getValue() == SupplyMode.Bulk50 && haveCount > item.getValue() / 2)
					|| (requestMode.getValue() == SupplyMode.Bulk100 && haveCount > 0)) {
				item.setValue(0);
				continue;
			}
			if (haveCount > 0) {
				item.setValue(item.getValue() - haveCount);
				// so that 1 damaged item can't satisfy a request for 2 other damage values.
				haveUndamaged.put(item.getKey().getUndamaged(), haveCount - item.getValue());
			}
			Integer requestedCount = requestedItems.get(item.getKey());
			if (requestedCount != null) {
				item.setValue(item.getValue() - requestedCount);
			}
		}

		service.getDebug().log("Supplier: Missing:   " + needed);

		setRequestFailed(false);

		//Make request
		for (Entry<ItemIdentifier, Integer> need : needed.entrySet()) {
			Integer amountRequested = need.getValue();
			if (amountRequested == null || amountRequested < 1) {
				continue;
			}
			int neededCount = amountRequested;
			if (!service.useEnergy(10)) {
				break;
			}

			boolean success = false;

			IAdditionalTargetInformation targetInformation = new SupplierTargetInformation();

			if (requestMode.getValue() != SupplyMode.Full) {
				service.getDebug().log("Supplier: Requesting partial: " + need.getKey().makeStack(neededCount));
				neededCount = RequestTree.requestPartial(need.getKey().makeStack(neededCount), this, targetInformation);
				service.getDebug().log("Supplier: Requested: " + need.getKey().makeStack(neededCount));
				if (neededCount > 0) {
					success = true;
				}
			} else {
				service.getDebug().log("Supplier: Requesting: " + need.getKey().makeStack(neededCount));
				success = RequestTree.request(need.getKey().makeStack(neededCount), this, null, targetInformation);
				if (success) {
					service.getDebug().log("Supplier: Request success");
				} else {
					service.getDebug().log("Supplier: Request failed");
				}
			}

			if (success) {
				Integer currentRequest = requestedItems.get(need.getKey());
				if (currentRequest == null) {
					requestedItems.put(need.getKey(), neededCount);
					service.getDebug().log("Supplier: Inserting Requested Items: " + neededCount);
				} else {
					requestedItems.put(need.getKey(), currentRequest + neededCount);
					service.getDebug()
							.log("Supplier: Raising Requested Items from: " + currentRequest + " to: " + currentRequest
									+ neededCount);
				}
			} else {
				setRequestFailed(true);
			}

		}
	}


	private void decreaseRequested(ItemIdentifierStack item) {
		final IPipeServiceProvider service = Objects.requireNonNull(this.service);
		int remaining = item.getStackSize();
		//see if we can get an exact match
		Integer count = requestedItems.get(item.getItem());
		if (count != null) {
			service.getDebug().log("Supplier: Exact match. Still missing: " + Math.max(0, count - remaining));
			if (count - remaining > 0) {
				requestedItems.put(item.getItem(), count - remaining);
			} else {
				requestedItems.remove(item.getItem());
			}
			remaining -= count;
		}
		if (remaining <= 0) {
			return;
		}
		//still remaining... was from fuzzyMatch on a crafter
		Iterator<Entry<ItemIdentifier, Integer>> it = requestedItems.entrySet().iterator();
		while (it.hasNext()) {
			Entry<ItemIdentifier, Integer> e = it.next();
			if (e.getKey().equalsWithoutNBT(item.getItem())) {
				int expected = e.getValue();
				service.getDebug().log("Supplier: Fuzzy match with" + e + ". Still missing: " + Math
						.max(0, expected - remaining));
				if (expected - remaining > 0) {
					e.setValue(expected - remaining);
				} else {
					it.remove();
				}
				remaining -= expected;
			}
			if (remaining <= 0) {
				return;
			}
		}
		//we have no idea what this is, log it.
		service.getDebug().log("Supplier: supplier got unexpected item " + item);
	}

	@Override
	public void itemLost(ItemIdentifierStack item, IAdditionalTargetInformation info) {
		final IPipeServiceProvider service = Objects.requireNonNull(this.service);
		service.getDebug().log("Supplier: Registered Item Lost: " + item);
		decreaseRequested(item);
	}

	@Override
	public void itemArrived(ItemIdentifierStack item, IAdditionalTargetInformation info) {
		final IPipeServiceProvider service = Objects.requireNonNull(this.service);
		service.getDebug().log("Supplier: Registered Item Arrived: " + item);
		decreaseRequested(item);
	}

	public void addStatusInformation(List<StatusEntry> status) {
		status.add(StatusEntry.of("Requested Items", requestedItems.entrySet()));
	}

	@Override
	public ModuleCoordinatesGuiProvider getPipeGuiProvider() {
		final boolean hasPatternUpgrade = hasPatternUpgrade();
		return NewGuiHandler.getGui(ActiveSupplierSlot.class)
				.setPatternUpgarde(hasPatternUpgrade)
				.setSlotArray(slotAssignmentPattern.stream().mapToInt(Integer::intValue).toArray())
				.setMode((hasPatternUpgrade ? patternMode.getValue() : requestMode.getValue()).ordinal())
				.setLimit(isLimited.getValue());
	}

	@Override
	public ModuleInHandGuiProvider getInHandGuiProvider() {
		return NewGuiHandler.getGui(ActiveSupplierInHand.class);
	}

	@Override
    public IRouter getRouter() {
		final IPipeServiceProvider service = Objects.requireNonNull(this.service);
		return service.getRouter();
	}

	@Override
	public void itemCouldNotBeSend(ItemIdentifierStack item, IAdditionalTargetInformation info) {
		itemLost(item, info);
	}

	@Override
	public int compareTo(IRequestItems other) {
		return Integer.compare(getID(), other.getID());
	}

	@Override
	public int getID() {
		return getRouter().getSimpleID();
	}

	public boolean hasPatternUpgrade() {
		return getUpgradeManager().hasPatternUpgrade();
	}

	public enum SupplyMode {
		Partial,
		Full,
		Bulk50,
		Bulk100,
		Infinite
	}

	public enum PatternMode {
		Partial,
		Full,
		Bulk50,
		Bulk100
	}

	public class PatternSupplierTargetInformation extends SupplierTargetInformation implements ITargetSlotInformation {

		private final int amount;
		private final int targetSlot;

		public PatternSupplierTargetInformation(int targetSlot, int amount) {
			super();
			this.targetSlot = targetSlot;
			this.amount = amount;

		}

		@Override
		public int getTargetSlot() {
			return targetSlot;
		}

		@Override
		public int getAmount() {
			return amount;
		}

		@Override
		public boolean isLimited() {
			return ModuleActiveSupplier.this.isLimited.getValue();
		}

	}

	public class SupplierTargetInformation extends ChassiTargetInformation {

		public SupplierTargetInformation() {
			super(getPositionInt());
		}

	}
}
