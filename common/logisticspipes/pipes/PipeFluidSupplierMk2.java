package logisticspipes.pipes;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import lombok.Getter;

import net.neoforged.neoforge.network.PacketDistributor;

import logisticspipes.interfaces.routing.IRequestFluid;
import logisticspipes.interfaces.routing.IRequireReliableFluidTransport;
import logisticspipes.network.to_client.FluidSupplierAmountMessage;
import logisticspipes.pipes.basic.fluid.FluidRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.request.RequestTree;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.transport.PipeFluidTransportLogistics;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.transfer.LPFluidTank;

public class PipeFluidSupplierMk2 extends FluidRoutedPipe implements IRequestFluid, IRequireReliableFluidTransport {

	private boolean lastRequestFailed = false;

	public enum MinMode {
		NONE(0),
		ONEBUCKET(1000),
		TWOBUCKET(2000),
		FIVEBUCKET(5000);

		@Getter
		private final int amount;

		MinMode(int amount) {
			this.amount = amount;
		}
	}

	public PipeFluidSupplierMk2(Item item) {
		super(item);
		throttleTime = 100;
		dummyInventory.addListener(inventory -> markTileDirty());
	}

	@Override
	public void sendFailed(FluidIdentifier value1, Integer value2) {
		liquidLost(value1, value2);
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Fast;
	}

	@Override
	public boolean canInsertFromSideToTanks() {
		return true;
	}

	@Override
	public boolean canInsertToTanks() {
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
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_LIQUIDSUPPLIER_MK2_TEXTURE;
	}

	@Override
	public boolean hasGenericInterests() {
		return true;
	}

	//from PipeFluidSupplierMk2
	private ItemIdentifierInventory dummyInventory = new ItemIdentifierInventory(1, "Fluid to keep stocked", 127, true);
	private int amount = 0;

	private final Map<FluidIdentifier, Integer> requestedItems = new HashMap<>();

	private boolean requestPartials = false;
	private MinMode bucketMinimum = MinMode.ONEBUCKET;

	@Override
	public void throttledUpdateEntity() {
		if (!isEnabled()) {
			return;
		}
		if (MainProxy.isClient(Objects.requireNonNull(container).getLevel())) {
			return;
		}
		super.throttledUpdateEntity();
		if (dummyInventory.getIDStackInSlot(0) == null) {
			return;
		}

		PipeFluidUtil.getAdjacentTanks(this, false).forEach(fluidHandlerDirectionPair -> {
			if (!fluidHandlerDirectionPair.getValue2().containsTanks()) {
				return;
			}

			//How much do I want?
			Map<FluidIdentifier, Integer> wantFluids = new HashMap<>();
			ItemIdentifierStack stack = dummyInventory.getIDStackInSlot(0);
			if (stack == null) return;
			FluidIdentifier fIdent = FluidIdentifier.get(stack.getItem());
			wantFluids.put(fIdent, amount);

			//How much do I have?
			HashMap<FluidIdentifier, Integer> haveFluids = new HashMap<>();

			//Check what is inside the connected tank
			fluidHandlerDirectionPair.getValue2().tanks()
					.map(tank -> FluidIdentifierStack.getFromStack(tank))
					.filter(Objects::nonNull)
					.forEach(fluid -> haveFluids.merge(fluid.getFluid(), fluid.getAmount(), Integer::sum));

			//What does our sided internal tank have
			int directionOrdinal = fluidHandlerDirectionPair.getValue1().getDirection().ordinal();
			if (directionOrdinal < ((PipeFluidTransportLogistics) transport).sideTanks.length) {
				LPFluidTank sideTank = ((PipeFluidTransportLogistics) transport).sideTanks[directionOrdinal];
				if (sideTank != null && sideTank.getFluid() != null && wantFluids.containsKey(FluidIdentifier.get(sideTank.getFluid()))) {
					haveFluids.merge(FluidIdentifier.get(sideTank.getFluid()), sideTank.getFluid().getAmount(), Integer::sum);
				}
			}

			//What does our center internal tank have
			LPFluidTank centerTank = ((PipeFluidTransportLogistics) transport).internalTank;
			if (centerTank != null && centerTank.getFluid() != null && wantFluids.containsKey(FluidIdentifier.get(centerTank.getFluid()))) {
				haveFluids.merge(FluidIdentifier.get(centerTank.getFluid()), centerTank.getFluid().getAmount(), Integer::sum);
			}

			//HashMap<Integer, Integer> needFluids = new HashMap<Integer, Integer>();
			//Reduce what I have and what have been requested already
			for (Entry<FluidIdentifier, Integer> liquidId : wantFluids.entrySet()) {
				Integer haveCount = haveFluids.get(liquidId.getKey());
				if (haveCount != null) {
					liquidId.setValue(liquidId.getValue() - haveCount);
				}
				//@formatter:off
						requestedItems.entrySet().stream()
								.filter(requestedItem -> requestedItem.getKey().equals(liquidId.getKey()))
								.forEach(requestedItem -> liquidId.setValue(liquidId.getValue() - requestedItem.getValue()));
						//@formatter:on
			}

			setRequestFailed(false);

			//Make request

			for (FluidIdentifier need : wantFluids.keySet()) {
				int countToRequest = wantFluids.get(need);
				if (countToRequest < 1) {
					continue;
				}
				if (bucketMinimum.getAmount() != 0 && countToRequest < bucketMinimum.getAmount()) {
					continue;
				}

				if (!useEnergy(11)) {
					break;
				}

				boolean success = false;

				if (requestPartials) {
					countToRequest = RequestTree.requestFluidPartial(need, countToRequest, this, null);
					if (countToRequest > 0) {
						success = true;
					}
				} else {
					success = RequestTree.requestFluid(need, countToRequest, this, null);
				}

				if (success) {
					Integer currentRequest = requestedItems.get(need);
					if (currentRequest == null) {
						requestedItems.put(need, countToRequest);
					} else {
						requestedItems.put(need, currentRequest + countToRequest);
					}
				} else {
					setRequestFailed(true);
				}
			}
		});
	}

	@Override
	public void deserialize(ValueInput input) {
		super.deserialize(input);
		dummyInventory.deserialize(input, "");
		requestPartials = input.getBooleanOr("requestpartials", false);
		amount = input.getIntOr("amount", 0);
		bucketMinimum = MinMode.values()[input.getByteOr("_bucketMinimum", (byte) 0)];
	}

	@Override
	public void serialize(ValueOutput output) {
		super.serialize(output);
		dummyInventory.serialize(output, "");
		output.putBoolean("requestpartials", requestPartials);
		output.putInt("amount", amount);
		output.putByte("_bucketMinimum", (byte) bucketMinimum.ordinal());
	}

	private void decreaseRequested(FluidIdentifier liquid, int remaining) {
		//see if we can get an exact match
		Integer count = requestedItems.get(liquid);
		if (count != null) {
			requestedItems.put(liquid, Math.max(0, count - remaining));
			remaining -= count;
		}
		if (remaining <= 0) {
			return;
		}
		//still remaining... was from fuzzyMatch on a crafter
		for (Entry<FluidIdentifier, Integer> e : requestedItems.entrySet()) {
			if (e.getKey().equals(liquid)) {
				int expected = e.getValue();
				e.setValue(Math.max(0, expected - remaining));
				remaining -= expected;
			}
			if (remaining <= 0) {
				return;
			}
		}
		//we have no idea what this is, log it.
		debug.log("liquid supplier got unexpected item " + liquid.toString());
	}

	@Override
	public void liquidLost(FluidIdentifier item, int amount) {
		decreaseRequested(item, amount);
	}

	@Override
	public void liquidArrived(FluidIdentifier item, int amount) {
		decreaseRequested(item, amount);
		delayThrottle();
	}

	@Override
	public void liquidNotInserted(FluidIdentifier item, int amount) {}

	public boolean isRequestingPartials() {
		return requestPartials;
	}

	public void setRequestingPartials(boolean value) {
		requestPartials = value;
		markTileDirty();
	}

	public MinMode getMinMode() {
		return bucketMinimum;
	}

	public void setMinMode(MinMode value) {
		bucketMinimum = value;
		markTileDirty();
	}

	@Override
	public void onWrenchClicked(Player entityplayer) {
		logisticspipes.network.NewGuiHandler.getGui(logisticspipes.network.guis.pipe.FluidSupplierMk2Gui.class)
				.setPosX(getX()).setPosY(getY()).setPosZ(getZ())
				.open(entityplayer);
	}

	public Container getDummyInventory() {
		return dummyInventory;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		if (MainProxy.isClient(Objects.requireNonNull(container).getLevel())) {
			this.amount = amount;
		}
	}

	public void changeFluidAmount(int change, Player player) {
		amount += change;
		if (amount <= 0) {
			amount = 0;
		}
		markTileDirty();
		if (player instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer, new FluidSupplierAmountMessage(getPos(), amount));
		}
	}

	@Override
	public boolean canReceiveFluid() {
		return false;
	}
}
