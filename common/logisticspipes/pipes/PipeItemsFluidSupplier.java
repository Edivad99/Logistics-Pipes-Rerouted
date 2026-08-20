package logisticspipes.pipes;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import javax.annotation.Nullable;
import logisticspipes.interfaces.ITankUtil;
import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.interfaces.routing.IRequireReliableTransport;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.request.RequestTree;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemServer;
import logisticspipes.transport.PipeTransportLogistics;
import logisticspipes.utils.CacheHolder.CacheTypes;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import network.rs485.logisticspipes.connection.LPNeighborTileEntityKt;
import network.rs485.logisticspipes.connection.NeighborTileEntity;
import network.rs485.logisticspipes.inventory.IItemIdentifierInventory;

public class PipeItemsFluidSupplier extends CoreRoutedPipe implements IRequestItems, IRequireReliableTransport {

	private boolean lastRequestFailed = false;
	private boolean requestPartials = false;

	private ItemIdentifierInventory dummyInventory = new ItemIdentifierInventory(9, "Fluids to keep stocked", 127);

	private final HashMap<ItemIdentifier, Integer> requestedItems = new HashMap<>();

	public PipeItemsFluidSupplier(Item item) {
		super(new PipeTransportLogistics(true) {

			@Override
			public boolean canPipeConnect(BlockEntity tile, Direction dir) {
				if (super.canPipeConnect(tile, dir)) {
					return true;
				}
				if (SimpleServiceLocator.pipeInformationManager.isItemPipe(tile)) {
					return false;
				}
				ITankUtil tank = PipeFluidUtil.INSTANCE.getTankUtilForTE(tile, dir.getOpposite());
				return tank != null && tank.containsTanks();
			}
		}, item);

		throttleTime = 100;
		dummyInventory.addListener(inventory -> markTileDirty());
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_LIQUIDSUPPLIER_TEXTURE;
	}

	/* TRIGGER INTERFACE */
	public boolean isRequestFailed() {
		return lastRequestFailed;
	}

	public void setRequestFailed(boolean value) {
		lastRequestFailed = value;
	}

	@Override
	public @Nullable LogisticsModule getLogisticsModule() {
		return null;
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Fast;
	}

	public void endReached(LPTravelingItemServer data, BlockEntity tile) {
		getCacheHolder().trigger(CacheTypes.Inventory);
		transport.markChunkModified(tile);
		notifyOfItemArival(data.getInfo());
		Direction orientation = data.output.getOpposite();
		if (getOriginalUpgradeManager().hasSneakyUpgrade()) {
			orientation = getOriginalUpgradeManager().getSneakyOrientation();
		}
		ITankUtil util = PipeFluidUtil.INSTANCE.getTankUtilForTE(tile, orientation);
		if (util == null) {
			return;
		}
		if (SimpleServiceLocator.pipeInformationManager.isItemPipe(tile)) {
			return;
		}
		final ItemIdentifierStack idStack = data.getItemIdentifierStack();
		if (idStack == null) {
			return;
		}
		FluidIdentifierStack liquidId = FluidUtil.getFluidContained(idStack.makeNormalStack()).map(FluidIdentifierStack::getFromStack).orElse(null);
		if (liquidId == null) {
			return;
		}
		while (idStack.getStackSize() > 0 && util.fill(liquidId, false) == liquidId.getAmount() && this.useEnergy(5)) {
			util.fill(liquidId, true);
			idStack.lowerStackSize(1);
			ItemStack remainder = idStack.getItem().makeNormalStack(1).getCraftingRemainder();
			if (!remainder.isEmpty()) {
				transport.sendItem(remainder);
			}
		}
	}

	@Override
	public boolean hasGenericInterests() {
		return true;
	}

	@Override
	public void throttledUpdateEntity() {
		if (!isEnabled()) {
			return;
		}

		if (MainProxy.isClient(getWorld())) {
			return;
		}
		super.throttledUpdateEntity();

		for (NeighborTileEntity<BlockEntity> neighbor : getAdjacent().fluidTanks()) {
			final ITankUtil tankUtil = LPNeighborTileEntityKt.getTankUtil(neighbor);
			if (tankUtil == null || !tankUtil.containsTanks()) {
				continue;
			}

			//How much do I want?
			Map<ItemIdentifier, Integer> wantContainers = dummyInventory.getItemsAndCount();
			HashMap<FluidIdentifier, Integer> wantFluids = new HashMap<>();
			for (Entry<ItemIdentifier, Integer> item : wantContainers.entrySet()) {
				ItemStack wantItem = item.getKey().makeNormalStack(1);
				FluidStack liquidStack = FluidUtil.getFluidContained(wantItem).orElse(null);
				if (liquidStack == null) {
					continue;
				}
				wantFluids.put(FluidIdentifier.get(liquidStack), item.getValue() * liquidStack.getAmount());
			}

			//How much do I have?
			HashMap<FluidIdentifier, Integer> haveFluids = new HashMap<>();

			tankUtil.tanks()
					.map(tank -> FluidIdentifierStack.getFromStack(tank))
					.filter(Objects::nonNull)
					.forEach(fluid -> {
						if (wantFluids.containsKey(fluid.getFluid())) {
							haveFluids.merge(fluid.getFluid(), fluid.getAmount(), Integer::sum);
						}
					});

			//HashMap<Integer, Integer> needFluids = new HashMap<Integer, Integer>();
			//Reduce what I have and what have been requested already
			for (Entry<FluidIdentifier, Integer> liquidId : wantFluids.entrySet()) {
				Integer haveCount = haveFluids.get(liquidId.getKey());
				if (haveCount != null) {
					liquidId.setValue(liquidId.getValue() - haveCount);
				}
			}
			for (Entry<ItemIdentifier, Integer> requestedItem : requestedItems.entrySet()) {
				ItemStack wantItem = requestedItem.getKey().makeNormalStack(1);
				FluidStack requestedFluidId = FluidUtil.getFluidContained(wantItem).orElse(null);
				if (requestedFluidId == null) {
					continue;
				}
				FluidIdentifier requestedFluid = FluidIdentifier.get(requestedFluidId);
				Integer want = wantFluids.get(requestedFluid);
				if (want != null) {
					wantFluids.put(requestedFluid, want - requestedItem.getValue() * requestedFluidId.getAmount());
				}
			}

			((PipeItemsFluidSupplier) Objects.requireNonNull(container).pipe).setRequestFailed(false);

			//Make request

			for (ItemIdentifier need : wantContainers.keySet()) {
				FluidStack requestedFluidId = FluidUtil.getFluidContained(need.makeNormalStack(1)).orElse(null);
				if (requestedFluidId == null) {
					continue;
				}
				if (!wantFluids.containsKey(FluidIdentifier.get(requestedFluidId))) {
					continue;
				}
				int countToRequest = wantFluids.get(FluidIdentifier.get(requestedFluidId)) / requestedFluidId.getAmount();
				if (countToRequest < 1) {
					continue;
				}

				if (!useEnergy(11)) {
					break;
				}

				boolean success = false;

				if (requestPartials) {
					countToRequest = RequestTree.requestPartial(need.makeStack(countToRequest), (IRequestItems) container.pipe, null);
					if (countToRequest > 0) {
						success = true;
					}
				} else {
					success = RequestTree.request(need.makeStack(countToRequest), (IRequestItems) container.pipe, null, null);
				}

				if (success) {
					Integer currentRequest = requestedItems.get(need);
					if (currentRequest == null) {
						requestedItems.put(need, countToRequest);
					} else {
						requestedItems.put(need, currentRequest + countToRequest);
					}
				} else {
					((PipeItemsFluidSupplier) container.pipe).setRequestFailed(true);
				}
			}
		}
	}

	@Override
	public void readFromNBT(CompoundTag nbttagcompound, HolderLookup.Provider provider) {
		super.readFromNBT(nbttagcompound, provider);
		dummyInventory.readFromNBT(nbttagcompound, provider, "");
		requestPartials = nbttagcompound.getBoolean("requestpartials");
	}

	@Override
	public void writeToNBT(CompoundTag nbttagcompound, HolderLookup.Provider provider) {
		super.writeToNBT(nbttagcompound, provider);
		dummyInventory.writeToNBT(nbttagcompound, provider, "");
		nbttagcompound.putBoolean("requestpartials", requestPartials);
	}

	private void decreaseRequested(ItemIdentifierStack item) {
		int remaining = item.getStackSize();
		//see if we can get an exact match
		Integer count = requestedItems.get(item.getItem());
		if (count != null) {
			requestedItems.put(item.getItem(), Math.max(0, count - remaining));
			remaining -= count;
		}
		if (remaining <= 0) {
			return;
		}
		//still remaining... was from fuzzyMatch on a crafter
		for (Entry<ItemIdentifier, Integer> e : requestedItems.entrySet()) {
			if (e.getKey().equalsWithoutNBT(item.getItem())) {
				int expected = e.getValue();
				e.setValue(Math.max(0, expected - remaining));
				remaining -= expected;
			}
			if (remaining <= 0) {
				return;
			}
		}
		//we have no idea what this is, log it.
		debug.log("liquid supplier got unexpected item " + item);
	}

	@Override
	public void itemLost(ItemIdentifierStack item, IAdditionalTargetInformation info) {
		decreaseRequested(item);
	}

	@Override
	public void itemArrived(ItemIdentifierStack item, IAdditionalTargetInformation info) {
		decreaseRequested(item);
		delayThrottle();
	}

	public boolean isRequestingPartials() {
		return requestPartials;
	}

	public void setRequestingPartials(boolean value) {
		requestPartials = value;
		markTileDirty();
	}

	@Override
	public void onWrenchClicked(Player entityplayer) {
		logisticspipes.network.NewGuiHandler.getGui(logisticspipes.network.guis.pipe.FluidSupplierGui.class)
				.setPosX(getX()).setPosY(getY()).setPosZ(getZ())
				.open(entityplayer);
	}

	/*** GUI ***/
	public IItemIdentifierInventory getDummyInventory() {
		return dummyInventory;
	}
}
