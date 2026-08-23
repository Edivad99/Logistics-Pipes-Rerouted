package logisticspipes.transport;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import logisticspipes.utils.transfer.LPFluidTank;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packets.pipe.PipeFluidUpdate;
import logisticspipes.pipes.basic.fluid.FluidRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.SafeTimeTracker;
import logisticspipes.utils.item.ItemIdentifierStack;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class PipeFluidTransportLogistics extends PipeTransportLogistics {

	public LPFluidTank[] sideTanks = new LPFluidTank[Direction.values().length];
	public LPFluidTank internalTank = new LPFluidTank(getInnerCapacity());

	public FluidStack[] renderCache = new FluidStack[7];

	public PipeFluidTransportLogistics() {
		super(true);
		for (Direction dir : Direction.values()) {
			sideTanks[dir.ordinal()] = new LPFluidTank(getSideCapacity());
		}
	}

	/**
	 * The fluid capability for one side.
	 *
	 * <p>The tank is already a {@code ResourceHandler}, so there is nothing to adapt -- the only
	 * reason for a wrapper is the pipe's own rule that fluid may not be pushed in while the pipe
	 * refuses it. Everything else delegates.</p>
	 */
	public ResourceHandler<FluidResource> getFluidResourceHandler(Direction face) {
		if (face.ordinal() >= sideTanks.length) {
			return null;
		}
		return new DelegatingResourceHandler<>(sideTanks[face.ordinal()]) {
			@Override
			public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
				return getFluidPipe().canReceiveFluid() ? super.insert(index, resource, amount, transaction) : 0;
			}
		};
	}

	private FluidRoutedPipe getFluidPipe() {
		return (FluidRoutedPipe) getPipe();
	}

	/** Returns the current fluid in the side tank for the given direction, or the internal tank if null. */
	public FluidStack getFluidInSideTank(Direction from) {
		if (from == null) return internalTank.getFluid();
		return sideTanks[from.ordinal()].getFluid();
	}

	public int fill(Direction from, FluidStack resource, boolean doFill) {
		if (from.ordinal() < Direction.values().length && getFluidPipe().canReceiveFluid()) {
			return sideTanks[from.ordinal()].fill(resource, doFill);
		} else {
			return 0;
		}
	}

	public FluidStack drain(Direction from, int maxDrain, boolean doDrain) {
		if (from.ordinal() < Direction.values().length) {
			return sideTanks[from.ordinal()].drain(maxDrain, doDrain);
		} else {
			return null;
		}
	}

	public FluidStack drain(Direction from, FluidStack resource, boolean doDrain) {
		if (sideTanks[from.ordinal()].getFluid() == null || !(FluidStack.isSameFluidSameComponents(sideTanks[from.ordinal()].getFluid(), resource))) {
			return new FluidStack(resource.getFluid(), 0);
		}
		return drain(from, resource.getAmount(), doDrain);
	}

	@Override
	public void deserialize(ValueInput input) {
		super.deserialize(input);

		for (Direction direction : Direction.values()) {
			sideTanks[direction.ordinal()].deserialize(input.childOrEmpty("tank[" + direction.ordinal() + "]"));
		}
		internalTank.deserialize(input.childOrEmpty("tank[middle]"));
	}

	@Override
	public void serialize(ValueOutput output) {
		super.serialize(output);

		for (Direction direction : Direction.values()) {
			output.putChild("tank[" + direction.ordinal() + "]", sideTanks[direction.ordinal()]);
		}
		output.putChild("tank[middle]", internalTank);
	}

	public int getInnerCapacity() {
		return 10000;
	}

	public int getSideCapacity() {
		return 5000;
	}

	@Override
	public void onNeighborBlockChange() {
		super.onNeighborBlockChange();

		for (Direction direction : Direction.values()) {
			if (!MainProxy.checkPipesConnections(container, container.getTile(PipeFluidTransportLogistics.orientations[direction.ordinal()]), PipeFluidTransportLogistics.orientations[direction.ordinal()])) {
				if (MainProxy.isServer(getWorld())) {
					FluidStack stack = sideTanks[direction.ordinal()].getFluid();
					if (stack != null && !stack.isEmpty()) {
						sideTanks[direction.ordinal()].setFluid(FluidStack.EMPTY);
						internalTank.fill(stack, true);
					}
				}
				if (renderCache[direction.ordinal()] != null) {
					renderCache[direction.ordinal()].setAmount(1);
				}
			}
		}
	}

	@Override
	public void updateEntity() {
		super.updateEntity();
		updateFluid();
	}

	/*
	 * BuildCraft Fluid Sync Code
	 */
	private final SafeTimeTracker tracker = new SafeTimeTracker(10);
	private long clientSyncCounter = 30;
	public byte initClient = 0;

	private static final Direction[] orientations = Direction.values();

	private void updateFluid() {
		if (MainProxy.isClient(getWorld())) {
			return;
		}
		if (tracker.markTimeIfDelay(getWorld())) {

			boolean init = false;
			if (++clientSyncCounter > 40) {
				clientSyncCounter = 0;
				init = true;
			}
			if (clientSyncCounter < 0) {
				clientSyncCounter = 0;
			}
			ModernPacket packet = computeFluidUpdate(init, true);
			if (packet != null) {
				MainProxy.sendPacketToAllWatchingChunk(container, packet);
			}
		}
	}

	/**
	 * Computes the PacketFluidUpdate packet for transmission to a client
	 *
	 * @param initPacket    everything is sent, no delta stuff ( first packet )
	 * @param persistChange The render cache change is persisted
	 * @return PacketFluidUpdate liquid update packet
	 */
	private ModernPacket computeFluidUpdate(boolean initPacket, boolean persistChange) {

		boolean changed = false;

		if (initClient > 0) {
			initClient--;
			if (initClient == 1) {
				changed = true;
			}
		}

		FluidStack[] renderCache = this.renderCache.clone();

		for (Direction dir : PipeFluidTransportLogistics.orientations) {
			FluidStack current;
			if (dir != null) {
				current = sideTanks[dir.ordinal()].getFluid();
			} else {
				current = internalTank.getFluid();
			}
			FluidStack prev = renderCache[dir.ordinal()];

			if (prev == null && (current == null || current.isEmpty())) {
				continue;
			} else if (prev == null) {
				changed = true;
				renderCache[dir.ordinal()] = current.copy();
				continue;
			} else if (current == null || current.isEmpty()) {
				changed = true;
				renderCache[dir.ordinal()] = null;
				continue;
			}

			if (prev.getFluid() != current.getFluid() || initPacket) {
				changed = true;
				renderCache[dir.ordinal()] = new FluidStack(current.getFluid(), renderCache[dir.ordinal()].getAmount());
			}

			if (prev.getAmount() != current.getAmount() || initPacket) {
				changed = true;
				renderCache[dir.ordinal()].setAmount(current.getAmount());
			}
		}

		if (persistChange) {
			this.renderCache = renderCache;
		}

		if (changed || initPacket) {
			return PacketHandler.getPacket(PipeFluidUpdate.class).setRenderCache(renderCache).setTilePos(container).setChunkDataPacket(initPacket);
		}

		return null;
	}

	@Override
	protected boolean isItemUnwanted(ItemIdentifierStack stack) {
		return false;
	}

	@Override
	protected boolean isPipeCheck(BlockEntity tile) {
		return SimpleServiceLocator.pipeInformationManager.isPipe(tile);
	}
}
