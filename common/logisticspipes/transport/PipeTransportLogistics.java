/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.transport;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jspecify.annotations.Nullable;

import logisticspipes.LPConstants;
import logisticspipes.LPEventListener;
import logisticspipes.LogisticsPipes;
import logisticspipes.api.provider.ILogisticsPowerProvider;
import logisticspipes.connection.special.SpecialBlockEntityConnection;
import logisticspipes.interfaces.IBufferItems;
import logisticspipes.interfaces.IItemAdvancedExistence;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.interfaces.ISpecialInsertion;
import logisticspipes.interfaces.ISubSystemPowerProvider;
import logisticspipes.interfaces.routing.ITargetSlotInformation;
import logisticspipes.inventory.IInventoryUtil;
import logisticspipes.inventory.InventoryUtilFactory;
import logisticspipes.inventory.transactor.ITransactor;
import logisticspipes.logisticspipes.IRoutedItem.TransportMode;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.network.TargetLookup;
import logisticspipes.network.to_client.pipe.PipeItemBufferMessage;
import logisticspipes.network.to_client.pipe.TravellingItemContentMessage;
import logisticspipes.network.to_client.pipe.TravellingItemPositionMessage;
import logisticspipes.network.to_server.pipe.RequestPipeContentMessage;
import logisticspipes.pipes.PipeItemsFluidSupplier;
import logisticspipes.pipes.PipeLogisticsChassis.ChassiTargetInformation;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.fluid.FluidRoutedPipe;
import logisticspipes.routing.ItemRoutingInformation;
import logisticspipes.routing.pathfinder.IPipeInformationProvider;
import logisticspipes.routing.pathfinder.PipeInformationManager;
import logisticspipes.ticks.LPTickHandler;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemClient;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemServer;
import logisticspipes.util.ItemStackLoader;
import logisticspipes.utils.CacheHolder.CacheTypes;
import logisticspipes.utils.OrientationsUtil;
import logisticspipes.utils.SyncList;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;
import logisticspipes.world.level.block.entity.LogisticsPowerJunctionBlockEntity;
import logisticspipes.world.level.block.entity.LogisticsSolidBlockEntity;

public class PipeTransportLogistics {

	@Data
	@AllArgsConstructor
	static class RoutingResult {

		private @Nullable Direction face;
		private boolean hasRoute;
	}

	private static final String TRAVELING_ITEMS_KEY = "travelingEntities";
	private static final String BUFFER_KEY = "buffercontents";

	private final int bufferTimeOut = 20 * 2; // 2 Seconds
	/**
	 * What is waiting to be retried. The client's copy, which {@link #setClientItemBuffer} fills for
	 * display, only has the stacks: the tick loop that reads the rest is server-only.
	 */
	public final SyncList<BufferedItem> itemBuffer = new SyncList<>();
	private @Nullable LevelChunk chunk;
	public LPItemList items = new LPItemList(this);
	public LogisticsGenericPipeBlockEntity container;
	public final boolean isRouted;
	public final int MAX_DESTINATION_UNREACHABLE_BUFFER = 30;

	public PipeTransportLogistics(boolean isRouted) {
		this.isRouted = isRouted;
	}

	public void initialize() {
		final Level level = getWorld();
		if (level != null && !level.isClientSide()) {
			// Cache the chunk for marking dirty, without forcing a load: initialize() runs while
			// the chunk is still being loaded, and getChunkAt() would re-enter the chunk system
			// asking for FULL status. getChunkNow returns null when it is not ready, which
			// markChunkModified already tolerates.
			chunk = level.getChunkSource().getChunkNow(
					SectionPos.blockToSectionCoord(container.getBlockPos().getX()),
					SectionPos.blockToSectionCoord(container.getBlockPos().getZ()));
			itemBuffer.syncTo(getWorld(), container.getBlockPos(), buffered -> new PipeItemBufferMessage(
				container.getBlockPos(),
				buffered.stream().map(BufferedItem::stack).toList()));
		}
	}

	public void markChunkModified(@Nullable BlockEntity blockEntity) {
		if (blockEntity != null && chunk != null) {
			// items are crossing a chunk boundary, mark both chunks modified
			if (container.getBlockPos().getX() >> 4 != blockEntity.getBlockPos().getX() >> 4 || container.getBlockPos().getZ() >> 4 != blockEntity.getBlockPos().getZ() >> 4) {
				chunk.markUnsaved();
				if (blockEntity instanceof LogisticsGenericPipeBlockEntity genericPipe &&
                    genericPipe.pipe != null &&
                    genericPipe.pipe.transport != null &&
                    genericPipe.pipe.transport.chunk != null) {
					genericPipe.pipe.transport.chunk.markUnsaved();
				} else {
					// Same reasoning as in initialize(): never force-load the neighbour's chunk
					// just to flag it dirty. If it is not loaded there is nothing to flag.
					LevelChunk neighbourChunk = getWorld().getChunkSource().getChunkNow(
							SectionPos.blockToSectionCoord(blockEntity.getBlockPos().getX()),
							SectionPos.blockToSectionCoord(blockEntity.getBlockPos().getZ()));
					if (neighbourChunk != null) {
						neighbourChunk.markUnsaved();
					}
				}
			}
		}
	}

	protected @Nullable CoreUnroutedPipe getPipe() {
		return container.pipe;
	}

	@Nullable
	protected CoreRoutedPipe getRoutedPipe() {
		if (!isRouted) {
			throw new UnsupportedOperationException("Can't use a Transport pipe as a routing pipe");
		}
		return (CoreRoutedPipe) container.pipe;
	}

	/** Replaces what the client thinks is buffered. The timeout and travelling item stay server-side. */
	public void setClientItemBuffer(List<ItemIdentifierStack> contents) {
		itemBuffer.clear();
		contents.forEach(stack -> itemBuffer.add(new BufferedItem(stack, 0, 0, null)));
	}

	public void updateEntity() {
		moveSolids();
		if (!getWorld().isClientSide()) {
			if (!itemBuffer.isEmpty()) {
				List<LPTravelingItem> toAdd = new LinkedList<>();
				Iterator<BufferedItem> iterator = itemBuffer.iterator();
				while (iterator.hasNext()) {
					BufferedItem next = iterator.next();
					final LPTravelingItemServer traveling = next.item;
					if (next.timeOut > 0) {
						next.timeOut--;
					} else if (traveling != null) {
						if (getRoutedPipe().getRouter().hasRoute(traveling.getDestination(), traveling.getTransportMode() == TransportMode.Active, traveling.getItemIdentifierStack().getItem()) || next.bufferCounter > MAX_DESTINATION_UNREACHABLE_BUFFER) {
							traveling.setBufferCounter(next.bufferCounter + 1);
							toAdd.add(traveling);
							iterator.remove();
						} else {
							next.bufferCounter++;
							next.timeOut = bufferTimeOut;
						}
					} else {
						LPTravelingItemServer item = new LPTravelingItemServer(next.stack);
						item.setDoNotBuffer(true);
						item.setBufferCounter(next.bufferCounter + 1);
						toAdd.add(item);
						iterator.remove();
					}
				}
				for (LPTravelingItem item : toAdd) {
					this.injectItem(item, Direction.UP);
				}
			}
			itemBuffer.sendUpdateToWatchers();
		}
	}

	public void dropBuffer() {
		Iterator<BufferedItem> iterator = itemBuffer.iterator();
		while (iterator.hasNext()) {
			ItemIdentifierStack next = iterator.next().stack;
            BlockPos pos = getPipe().getPos();
			getWorld().addFreshEntity(new ItemEntity(getWorld(), pos.getX(), pos.getY(), pos.getZ(),
					next.makeNormalStack()));
			iterator.remove();
		}
	}

	public int injectItem(LPTravelingItemServer item, Direction inputOrientation) {
		return injectItem((LPTravelingItem) item, inputOrientation);
	}

	public float getPipeLength() {
		return 1.0F;
	}

	public double getDistanceWeight() {
		return 1.0D;
	}

	public float getYawDiff(LPTravelingItem item) {
		return 0.0F;
	}

	public int injectItem(LPTravelingItem item, @Nullable Direction inputOrientation) {
		if (item.isCorrupted()) {
			// Safe guard - if for any reason the item is corrupted at this
			// stage, avoid adding it to the pipe to avoid further exceptions.
			return 0;
		}
		getPipe().triggerDebug();

		int originalCount = item.getItemIdentifierStack().getStackSize();

		item.input = inputOrientation;

		if (!getWorld().isClientSide()) {
			readjustSpeed((LPTravelingItemServer) item);
			ItemRoutingInformation info1 = ((LPTravelingItemServer) item).getInfo().clone();
			RoutingResult result = resolveDestination((LPTravelingItemServer) item);
			item.output = result.getFace();
			if (!result.hasRoute) {
				return 0;
			}
			getPipe().debug.log("Injected Item: [" + item.input + ", " + item.output + "] (" + info1);
		} else {
			item.output = null;
		}

		if (item.getPosition() >= getPipeLength()) {
			reachedEnd(item);
		} else {
			items.add(item);

			if (!getWorld().isClientSide() && !getPipe().isOpaque() && item.getItemIdentifierStack().getStackSize() > 0) {
				sendItemPacket((LPTravelingItemServer) item);
			}
		}
		return originalCount - item.getItemIdentifierStack().getStackSize();
	}

	public int injectItem(IRoutedItem item, Direction inputOrientation) {
		return injectItem((LPTravelingItem) item, inputOrientation);
	}

	/**
	 * emit the supplied item. This function assumes ownership of the item, and
	 * you may assume that it is now either buffered by the pipe or moving
	 * through the pipe.
	 *
	 * @param item
	 *            the item that just bounced off an inventory. In the case of a
	 *            pipe with a buffer, this function will alter item.
	 */
	protected void reverseItem(LPTravelingItemServer item) {
		if (item.isCorrupted()) {
			// Safe guard - if for any reason the item is corrupted at this
			// stage, avoid adding it to the pipe to avoid further exceptions.
			return;
		}

		if (getPipe() instanceof IBufferItems) {
			item.getItemIdentifierStack().setStackSize(((IBufferItems) getPipe()).addToBuffer(item.getItemIdentifierStack(), item.getAdditionalTargetInformation()));
			if (item.getItemIdentifierStack().getStackSize() <= 0) {
				return;
			}
		}

		// Assign new ID to update ItemStack content
		item.id = item.getNextId();

		if (item.getPosition() >= getPipeLength()) {
			item.setPosition(item.getPosition() - getPipeLength());
		}

		item.input = item.output.getOpposite();

		readjustSpeed(item);
		RoutingResult result = resolveDestination(item);
		item.output = result.getFace();
		if (!result.hasRoute) {
			return;
		} else if (item.output == null) {
			dropItem(item);
			return;
		}

		items.unscheduleRemoval(item);
		if (!getPipe().isOpaque()) {
			sendItemPacket(item);
		}
	}

	public RoutingResult resolveDestination(LPTravelingItemServer data) {
		if (isRouted) {
			return resolveRoutedDestination(data);
		} else {
			return resolveUnroutedDestination(data);
		}
	}

	public RoutingResult resolveUnroutedDestination(LPTravelingItemServer data) {
		List<Direction> dirs = new ArrayList<>(Arrays.asList(Direction.values()));
		dirs.remove(data.input.getOpposite());
		Iterator<Direction> iter = dirs.iterator();
		while (iter.hasNext()) {
			Direction dir = iter.next();
			BlockPos pos = getPipe().getPos().relative(dir);
			BlockEntity tile = getWorld().getBlockEntity(pos);
			if (!PipeInformationManager.isPipe(tile)) {
				iter.remove();
			} else if (!canPipeConnect(tile, dir)) {
				iter.remove();
			} else if (tile instanceof LogisticsGenericPipeBlockEntity && !((LogisticsGenericPipeBlockEntity) tile).canConnect(container, dir.getOpposite(), false)) {
				iter.remove();
			}
		}
		if (dirs.isEmpty()) {
			return new RoutingResult(null, false);
		}
		int num = new Random().nextInt(dirs.size());
		return new RoutingResult(dirs.get(num), true);
	}

	public RoutingResult resolveRoutedDestination(LPTravelingItemServer data) {

		Direction blocked = null;

		if (data.getDestinationUUID() == null) {
			ItemIdentifierStack stack = data.getItemIdentifierStack();
			ItemRoutingInformation result = getRoutedPipe().getQueuedForItemStack(stack);
			if (result != null) {
				data.setInformation(result);
				data.getInfo().setItem(stack);
				blocked = data.input.getOpposite();
			}
		}

		if (data.getItemIdentifierStack() != null) {
			getRoutedPipe().relayedItem(data.getItemIdentifierStack().getStackSize());
		}

		if (data.getDestination() >= 0 && !getRoutedPipe().getRouter().hasRoute(data.getDestination(), data.getTransportMode() == TransportMode.Active, data.getItemIdentifierStack().getItem()) && data.getBufferCounter() < MAX_DESTINATION_UNREACHABLE_BUFFER) {
			itemBuffer.add(new BufferedItem(data.getItemIdentifierStack(), bufferTimeOut, data.getBufferCounter(), data));
			return new RoutingResult(null, false);
		}

		Direction value;
		if (getRoutedPipe().stillNeedReplace() || getRoutedPipe().initialInit()) {
			data.setDoNotBuffer(false);
			value = null;
		} else {
			value = getRoutedPipe().getRouteLayer().getOrientationForItem(data, blocked);
		}
		if (value == null && getWorld().isClientSide()) {
			return new RoutingResult(null, true);
		}

		if (value == null && !data.getDoNotBuffer() && data.getBufferCounter() < 5) {
			itemBuffer.add(new BufferedItem(data.getItemIdentifierStack(), bufferTimeOut, data.getBufferCounter(), null));
			return new RoutingResult(null, false);
		}

		if (value != null && !getRoutedPipe().getRouter().isRoutedExit(value)) {
			if (isItemUnwanted(data.getItemIdentifierStack())) {
				return new RoutingResult(null, false);
			}
		}

		data.resetDelay();

		return new RoutingResult(value, true);
	}

	/**
	 * Drops what {@link #serialize} writes for the server alone from a copy of the saved data meant
	 * for a client. The traveling items there are server items, and the client learns about items
	 * and the buffer through their own packets.
	 */
	public static void removeServerOnlyData(CompoundTag tag) {
		tag.remove(TRAVELING_ITEMS_KEY);
		tag.remove(BUFFER_KEY);
	}

	public void deserialize(ValueInput input) {

		for (ValueInput dataTag : input.childrenListOrEmpty(TRAVELING_ITEMS_KEY)) {
			try {
				LPTravelingItem item = new LPTravelingItemServer(dataTag);

				if (item.isCorrupted()) {
					LogisticsPipes.LOG.warn("Dropped a traveling item whose stack could not be read");
					continue;
				}

				items.scheduleLoad(item);
			} catch (Throwable t) {
				// It may be the case that entities cannot be reloaded between two versions.
				LogisticsPipes.LOG.warn("Dropped a traveling item that could not be loaded", t);
			}
		}

		itemBuffer.clear();

		for (ValueInput bufferEntry : input.childrenListOrEmpty(BUFFER_KEY)) {
			ItemStack stack = ItemStackLoader.loadItemStack(bufferEntry);
			itemBuffer.add(new BufferedItem(ItemIdentifierStack.getFromStack(stack), bufferTimeOut, 0, null));
		}

	}

	public void serialize(ValueOutput output) {

		ValueOutput.ValueOutputList travelingEntities = output.childrenList(TRAVELING_ITEMS_KEY);
		for (LPTravelingItem item : items.itemsToSave()) {
			if (item instanceof LPTravelingItemServer) {
				((LPTravelingItemServer) item).serialize(travelingEntities.addChild());
			}
		}

		ValueOutput.ValueOutputList bufferContents = output.childrenList(BUFFER_KEY);
		for (BufferedItem buffered : itemBuffer) {
			ItemStackLoader.saveItemStack(bufferContents.addChild(), buffered.stack.makeNormalStack());
		}

	}

	public void readjustSpeed(LPTravelingItemServer item) {
		float defaultBoost = switch (item.getTransportMode()) {
            case Default -> 20F;
            case Passive -> 25F;
            case Active -> 30F;
            case Unknown -> 20F;
        };

        if (isRouted) {
            int speedUpgradeCount = getRoutedPipe().getUpgradeManager().getSpeedUpgradeCount();
			float multiplyerSpeed = 1.0F + (0.02F * speedUpgradeCount);
			float multiplyerPower = 1.0F + (0.03F * speedUpgradeCount);

			float add = Math.max(item.getSpeed(), LPConstants.PIPE_NORMAL_SPEED * defaultBoost * multiplyerPower) - item.getSpeed();
			if (getRoutedPipe().useEnergy((int) (add * 50 + 0.5))) {
				item.setSpeed(Math.min(Math.max(item.getSpeed(), LPConstants.PIPE_NORMAL_SPEED * defaultBoost * multiplyerSpeed), 1.0F));
			}
		}
	}

	protected void handleTileReachedServer(LPTravelingItemServer arrivingItem, @Nullable BlockEntity tile, @Nullable Direction dir) {
		handleTileReachedServer_internal(arrivingItem, tile, dir);
	}

	protected final void handleTileReachedServer_internal(LPTravelingItemServer arrivingItem, @Nullable BlockEntity tile, @Nullable Direction dir) {
		if (getPipe() instanceof PipeItemsFluidSupplier pipeItemsFluidSupplier) {
			pipeItemsFluidSupplier.endReached(arrivingItem, tile);
			if (arrivingItem.getItemIdentifierStack().getStackSize() <= 0) {
				return;
			}
		}

		markChunkModified(tile);
		if (arrivingItem.getInfo() != null && arrivingItem.getArrived() && isRouted) {
			getRoutedPipe().notifyOfItemArrival(arrivingItem.getInfo());
		}
		if (getPipe() instanceof FluidRoutedPipe) {
			if (((FluidRoutedPipe) getPipe()).endReached(arrivingItem, tile)) {
				return;
			}
		}
		boolean isSpecialConnectionInformationTransition = false;
		if (SpecialBlockEntityConnection.get().needsInformationTransition(tile)) {
			isSpecialConnectionInformationTransition = true;
			SpecialBlockEntityConnection.get().transmit(Objects.requireNonNull(tile), arrivingItem);
		}
		if (PipeInformationManager.isPipe(tile)) {
			if (passToNextPipe(arrivingItem, Objects.requireNonNull(tile))) {
				return;
			}
		} else {
			IInventoryUtil util = InventoryUtilFactory.get().getInventoryUtil(tile, dir.getOpposite());
			if (util != null && isRouted) {
				getRoutedPipe().getCacheHolder().trigger(CacheTypes.Inventory);

				// items.scheduleRemoval(arrivingItem);
				// destroy the item on exit if it isn't exitable
				if (!isSpecialConnectionInformationTransition && isItemUnwanted(arrivingItem.getItemIdentifierStack())) {
					return;
				}
				// last chance for chassi to back out
				if (arrivingItem.getTransportMode() != TransportMode.Active && !getRoutedPipe().getTransportLayer().stillWantItem(arrivingItem)) {
					reverseItem(arrivingItem);
					return;
				}
				final ISlotUpgradeManager slotManager;
				if (arrivingItem.getInfo().targetInfo instanceof ChassiTargetInformation chassiTarget) {
					slotManager = getRoutedPipe().getUpgradeManager(ModulePositionType.SLOT, chassiTarget.getModuleSlot());
				} else {
					if (container.pipe instanceof PipeLogisticsChassis) {
						// An item bounced back into the chassis's own inventory, or one whose route was
						// lost, no longer belongs to a module; it goes in under the pipe's upgrades.
						LogisticsPipes.LOG.debug("[ItemInsertion] info not for chassis pipe: {}", arrivingItem);
					}
					slotManager = getRoutedPipe().getOriginalUpgradeManager();
				}
				if (arrivingItem.getAdditionalTargetInformation() instanceof ITargetSlotInformation information) {

					if (util instanceof ISpecialInsertion) {
						int slot = information.getTargetSlot();
						int amount = information.getAmount();
						if (util.getContainerSize() > slot) {
							ItemStack content = util.getItem(slot);
							ItemStack toAdd = arrivingItem.getItemIdentifierStack().makeNormalStack();
							final int amountLeft = Math.max(0, amount - content.getCount());
							toAdd.setCount(Math.min(toAdd.getCount(), amountLeft));
							if (toAdd.getCount() > 0) {
								if (util.getContainerSize() > slot) {
									int added = ((ISpecialInsertion) util).addToSlot(toAdd, slot);
									arrivingItem.getItemIdentifierStack().lowerStackSize(added);
								}
							}
						}
						if (information.isLimited()) {
							if (arrivingItem.getItemIdentifierStack().getStackSize() > 0) {
								reverseItem(arrivingItem);
							}
							return;
						}
					}
				}
				// sneaky insertion
				if (!getRoutedPipe().getUpgradeManager().hasCombinedSneakyUpgrade() || slotManager.hasOwnSneakyUpgrade()) {
					Direction insertion = arrivingItem.output.getOpposite();
					if (slotManager.hasSneakyUpgrade()) {
						insertion = slotManager.getSneakyOrientation();
					}
					if (insertArrivingItem(arrivingItem, tile, insertion)) return;
				} else {
					var dirs = getRoutedPipe().getUpgradeManager().getCombinedSneakyOrientation();
					if (dirs != null) {
						for (Direction insertion : dirs) {
							if (insertion == null) {
								continue;
							}
							if (insertArrivingItem(arrivingItem, tile, insertion)) return;
						}
					}
				}

				if (arrivingItem.getItemIdentifierStack().getStackSize() > 0) {
					reverseItem(arrivingItem);
				}
				return;// the item is handled
			}// end of insert into AbstractContainerMenu
		}
		dropItem(arrivingItem);
	}

	/**
	 * @return true, if every item has been inserted and otherwise false.
	 */
	private boolean insertArrivingItem(LPTravelingItemServer arrivingItem, BlockEntity tile, @Nullable Direction insertion) {
		// A sneaky upgrade points at a side of its own choosing, which need not be the side the item
		// arrived from -- and a block can expose an item handler on one face and not another. No
		// handler there means nothing was inserted, so the caller moves on to the next side.
		final ITransactor transactor = InventoryUtilFactory.get().getTransactor(tile, insertion);
		if (transactor == null) {
			return false;
		}
		ItemStack added = transactor.add(arrivingItem.getItemIdentifierStack().makeNormalStack(), insertion, true);

		if (!added.isEmpty()) {
			arrivingItem.getItemIdentifierStack().lowerStackSize(added.getCount());
			arrivingItem.setBufferCounter(0);
		}

		ItemRoutingInformation info = arrivingItem.getInfo();
		final boolean isSplitStack = arrivingItem.getItemIdentifierStack().getStackSize() > 0;
		if (isSplitStack) {
			// we have some leftovers, we are splitting the stack, we need to clone the info
			info = info.clone();
		}
		if(added.isEmpty()){
			info.getItem().setStackSize(0);
		} else {
			info.getItem().setStackSize(added.getCount());
		}

		inventorySystemConnectorHook(info, tile);

		// back to normal code, break if we've inserted everything, all items disposed of.
		return !isSplitStack;
	}

	protected void handleTileReachedClient(LPTravelingItemClient arrivingItem, @Nullable BlockEntity tile, @Nullable Direction dir) {
		if (PipeInformationManager.isPipe(tile)) {
			passToNextPipe(arrivingItem, tile);
		}
		// Just ignore any other case
	}

	protected boolean isItemUnwanted(ItemIdentifierStack itemIdentifierStack) {
		if (itemIdentifierStack != null && itemIdentifierStack.makeNormalStack().getItem() instanceof IItemAdvancedExistence) {
			return !((IItemAdvancedExistence) itemIdentifierStack.makeNormalStack().getItem()).canExistInNormalInventory(itemIdentifierStack.makeNormalStack());
		}
		return false;
	}

	protected void inventorySystemConnectorHook(ItemRoutingInformation info, BlockEntity tile) {}

	public boolean canPipeConnect(BlockEntity tile, Direction side) {
		return canPipeConnect_internal(tile, side);
	}

	public final boolean canPipeConnect_internal(BlockEntity tile, Direction side) {
		if (tile instanceof LogisticsGenericPipeBlockEntity genericPipeBlockEntity) {
			if (genericPipeBlockEntity.pipe != null && genericPipeBlockEntity.pipe.isHSTube() && !genericPipeBlockEntity.pipe.actAsNormalPipe()) {
				return false;
			}
		}
		if (isRouted) {
			if (tile instanceof ILogisticsPowerProvider || tile instanceof ISubSystemPowerProvider) {
				Direction ori = OrientationsUtil.getOrientationOfTilewithTile(container, tile);
				if (ori != null) {
					return !((tile instanceof LogisticsPowerJunctionBlockEntity || tile instanceof ISubSystemPowerProvider) && ori.getAxis() == Direction.Axis.Y);
				}
			}
			IInventoryUtil util = InventoryUtilFactory.get().getInventoryUtil(tile, side.getOpposite());
			if (util != null) {
				return true;
			}
			if (tile instanceof LogisticsSolidBlockEntity) {
				return true;
			}
			return isPipeCheck(tile);
		} else {
			return isPipeCheck(tile);
		}
	}

	protected boolean isPipeCheck(BlockEntity tile) {
		return PipeInformationManager.isPipe(tile);
	}

	protected void reachedEnd(LPTravelingItem item) {
		BlockEntity tile = container.getBlockEntity(item.output);
		if (items.scheduleRemoval(item)) {
			if (!getWorld().isClientSide()) {
				handleTileReachedServer((LPTravelingItemServer) item, tile, item.output);
			} else {
				handleTileReachedClient((LPTravelingItemClient) item, tile, item.output);
			}
		}
	}

	private void moveSolids() {
		items.flush();
		items.scheduleAdd();
		for (LPTravelingItem item : items) {
			if (item.lastTicked >= LPTickHandler.getGlobalTick()) {
				continue;
			}
			item.lastTicked = LPTickHandler.getGlobalTick();
			item.addAge();
			item.setPosition(item.getPosition() + item.getSpeed());
			if (hasReachedEnd(item)) {
				if (item.output == null) {
					if (!getWorld().isClientSide()) {
						dropItem((LPTravelingItemServer) item);
					}
					items.scheduleRemoval(item);
					continue;
				}
				reachedEnd(item);
			}
		}
		items.removeScheduledItems();
		items.addScheduledItems();
	}

	protected boolean passToNextPipe(LPTravelingItem item, BlockEntity tile) {
		IPipeInformationProvider information = PipeInformationManager.getInformationProviderFor(tile);
		if (information != null) {
			item.setPosition(item.getPosition() - getPipeLength());
			item.setYaw(item.getYaw() + (getYawDiff(item)));
			return information.acceptItem(item, container);
		}
		return false;
	}

	private void dropItem(LPTravelingItemServer item) {
		if (getWorld().isClientSide()) {
			return;
		}
		item.setSpeed(0.05F);
		item.setContainer(container);
		ItemEntity entity = item.toEntityItem();
		if (entity != null) {
			getWorld().addFreshEntity(entity);
		}
	}

	protected boolean hasReachedEnd(LPTravelingItem item) {
		return item.getPosition() >= ((item.output == null) ? 0.75F : getPipeLength());
	}

	protected void neighborChange() {}

	public NonNullList<ItemStack> dropContents() {
		NonNullList<ItemStack> list = NonNullList.create();
		if (!getWorld().isClientSide()) {
			for (LPTravelingItem item : items) {
				list.add(item.getItemIdentifierStack().makeNormalStack());
			}
		}
		return list;
	}

	public boolean delveIntoUnloadedChunks() {
		return true;
	}

	private void sendItemPacket(LPTravelingItemServer item) {
		if (LPEventListener.isAnyoneWatching(container.getBlockPos())) {
			if (!LPTravelingItem.clientSideKnownIDs.get(item.getId())) {
				TargetLookup.sendToChunkWatchers(container,
						new TravellingItemContentMessage(item.getId(), item.getItemIdentifierStack()));
				LPTravelingItem.clientSideKnownIDs.set(item.getId());
			}
			TargetLookup.sendToChunkWatchers(container, new TravellingItemPositionMessage(
				container.getBlockPos(),
				item.getId(),
				new TravellingItemPositionMessage.Motion(item.getPosition(), item.getSpeed(), item.getYaw()),
				Optional.ofNullable(item.input),
				Optional.ofNullable(item.output)));
		}
	}

	public void handleItemPositionPacket(int travelId, Direction input, Direction output, float speed, float position, float yaw) {
		WeakReference<LPTravelingItemClient> ref = LPTravelingItem.clientList.get(travelId);
		LPTravelingItemClient item = null;
		if (ref != null) {
			item = ref.get();
		}
		if (item == null) {
			sendItemContentRequest(travelId);
			item = new LPTravelingItemClient(travelId, position, input, output, yaw);
			item.setSpeed(speed);
			LPTravelingItem.clientList.put(travelId, new WeakReference<>(item));
			// Prevent double-move only for newly created items (they haven't been ticked yet).
			item.lastTicked = LPTickHandler.getGlobalTick();
		} else {
			if (item.getContainer() instanceof LogisticsGenericPipeBlockEntity oldPipe) {
				oldPipe.pipe.transport.items.scheduleRemoval(item);
				oldPipe.pipe.transport.items.removeScheduledItems();
				// Compute a continuous position rather than snapping to the server's stale value.
				// In 1.20.1 the integrated server runs on a separate thread, so packets arrive
				// ~1 tick late.  Hard-snapping causes a backward jump + 1-tick pause at every
				// pipe boundary.
				float adjustedPosition;
				if (!oldPipe.getBlockPos().equals(container.getBlockPos())) {
					// Item is crossing into this pipe on the client side.
					// Derive position from old-pipe offset for visual continuity.
					adjustedPosition = Math.max(0.0F, item.getPosition() - oldPipe.pipe.transport.getPipeLength());
				} else {
					// Already in the correct pipe — never go backward.
					adjustedPosition = Math.max(position, item.getPosition());
				}
				item.updateInformation(input, output, speed, adjustedPosition, yaw);
			} else {
				item.updateInformation(input, output, speed, position, yaw);
			}
			// Do NOT set lastTicked for existing mid-flight items.
			// Setting it causes a 1-tick advancement pause at every pipe boundary,
			// which is the root cause of the visible hiccup.
		}
		if (items.get(travelId) == null) {
			items.add(item);
		}
		//getPipe().spawnParticle(Particles.OrangeParticle, 1);
	}

	private void sendItemContentRequest(int travelId) {
		ClientPacketDistributor.sendToServer(new RequestPipeContentMessage(travelId));
	}

	public void sendItem(ItemStack stackToSend) {
		this.injectItem(new LPTravelingItemServer(stackToSend), Direction.UP);
	}

    @Nullable
	public Level getWorld() {
		return container.getLevel();
	}

	public void onNeighborBlockChange() {}

	public void onBlockPlaced() {}

	public void setTile(LogisticsGenericPipeBlockEntity tile) {
		container = tile;
	}

    @Nullable
	public CoreUnroutedPipe getNextPipe(Direction output) {
		if (container.getBlockEntity(output) instanceof LogisticsGenericPipeBlockEntity genericPipeBlockEntity) {
			return genericPipeBlockEntity.pipe;
		}
		return null;
	}

	/** An item waiting in the pipe to be sent on again. */
	public static final class BufferedItem {

		private final ItemIdentifierStack stack;
		/** Ticks left before the next try. */
		private int timeOut;
		/** How many times the item has been buffered so far. */
		private int bufferCounter;
		/** The traveling item to send on, or null to make a fresh one from the stack. */
		private final @Nullable LPTravelingItemServer item;

		private BufferedItem(ItemIdentifierStack stack, int timeOut, int bufferCounter, @Nullable LPTravelingItemServer item) {
			this.stack = stack;
			this.timeOut = timeOut;
			this.bufferCounter = bufferCounter;
			this.item = item;
		}

		public ItemIdentifierStack stack() {
			return stack;
		}
	}
}
