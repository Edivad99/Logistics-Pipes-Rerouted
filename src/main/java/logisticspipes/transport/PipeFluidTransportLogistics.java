package logisticspipes.transport;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import org.jspecify.annotations.Nullable;

import logisticspipes.network.TargetLookup;
import logisticspipes.network.to_client.pipe.PipeFluidUpdateMessage;
import logisticspipes.pipes.basic.fluid.FluidRoutedPipe;
import logisticspipes.routing.pathfinder.PipeInformationManager;
import logisticspipes.utils.SafeTimeTracker;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.transfer.LPFluidTank;

/**
 * The transport of the fluid pipes: a tank on each side, where fluid comes in from and goes out to
 * the neighbors, and a bigger one in the middle. Fluid travels between pipes packed in items, like
 * anything else.
 */
public class PipeFluidTransportLogistics extends PipeTransportLogistics {

    /**
     * The slot of {@link #renderCache} for {@link #internalTank}; the others are the side tanks, by direction.
     */
    public static final int CENTER = 6;

    public LPFluidTank[] sideTanks = new LPFluidTank[Direction.values().length];
    public LPFluidTank internalTank = new LPFluidTank(getInnerCapacity());

    /**
     * What the clients were last told the tanks hold, by {@link #CENTER} and side; on a client, what
     * it was told. Null for an empty tank.
     */
    public @Nullable FluidStack[] renderCache = new FluidStack[CENTER + 1];

    private final SafeTimeTracker tracker = new SafeTimeTracker(10);
    private long clientSyncCounter = 30;

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
        return new DelegatingResourceHandler<>(sideTanks[face.ordinal()]) {
            @Override
            public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
                return canReceiveFluid() ? super.insert(index, resource, amount, transaction) : 0;
            }
        };
    }

    private boolean canReceiveFluid() {
        return getPipe() instanceof FluidRoutedPipe pipe && pipe.canReceiveFluid();
    }

    /**
     * The fluid in the side tank for the given direction.
     */
    public FluidStack getFluidInSideTank(Direction from) {
        return sideTanks[from.ordinal()].getFluid();
    }

    public int fill(Direction from, FluidStack resource, boolean doFill) {
        return canReceiveFluid() ? sideTanks[from.ordinal()].fill(resource, doFill) : 0;
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

    /**
     * Moves the fluid of a side that lost its connection into the middle tank.
     */
    @Override
    public void onNeighborBlockChange() {
        super.onNeighborBlockChange();
        for (Direction direction : Direction.values()) {
            if (PipeInformationManager.canConnect(container, container.getBlockEntity(direction), direction)) {
                continue;
            }
            if (!getWorld().isClientSide()) {
                FluidStack stack = sideTanks[direction.ordinal()].getFluid();
                if (!stack.isEmpty()) {
                    sideTanks[direction.ordinal()].setFluid(FluidStack.EMPTY);
                    internalTank.fill(stack, true);
                }
            }
            FluidStack rendered = renderCache[direction.ordinal()];
            if (rendered != null) {
                rendered.setAmount(1);
            }
        }
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        updateFluid();
    }

    /**
     * Every half second, tells the clients watching the pipe what changed in its tanks; every 20
     * seconds, all of it.
     */
    private void updateFluid() {
        if (getWorld().isClientSide() || !tracker.markTimeIfDelay(getWorld())) {
            return;
        }
        boolean everything = false;
        if (++clientSyncCounter > 40) {
            clientSyncCounter = 0;
            everything = true;
        }
        if (updateRenderCache() || everything) {
            List<FluidStack> sides = new ArrayList<>(renderCache.length);
            for (FluidStack side : renderCache) {
                sides.add(side == null ? FluidStack.EMPTY : side);
            }
            TargetLookup.sendToChunkWatchers(container, new PipeFluidUpdateMessage(container.getBlockPos(), sides));
        }
    }

    /**
     * Brings {@link #renderCache} up to date with the tanks.
     *
     * @return whether anything changed
     */
    private boolean updateRenderCache() {
        boolean changed = false;
        for (int slot = 0; slot < renderCache.length; slot++) {
            FluidStack current = slot == CENTER ? internalTank.getFluid() : sideTanks[slot].getFluid();
            FluidStack previous = renderCache[slot];

            if (current.isEmpty()) {
                if (previous != null) {
                    renderCache[slot] = null;
                    changed = true;
                }
            } else if (previous == null || previous.getFluid() != current.getFluid()) {
                renderCache[slot] = current.copy();
                changed = true;
            } else if (previous.getAmount() != current.getAmount()) {
                previous.setAmount(current.getAmount());
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Lets the fluid containers in: they are how fluid travels between pipes, and the pipe pours
     * them out into the tanks once they arrive.
     */
    @Override
    protected boolean isItemUnwanted(ItemIdentifierStack stack) {
        return false;
    }
}
