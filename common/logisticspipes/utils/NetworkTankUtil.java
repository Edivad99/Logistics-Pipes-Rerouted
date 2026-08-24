package logisticspipes.utils;

import java.util.stream.Stream;

import net.neoforged.neoforge.fluids.FluidStack;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.ITankUtil;

/**
 * An {@link ITankUtil} over a storage network, which holds fluids without any notion of a tank.
 *
 * <p>Deliberately not built on {@link TankUtil}: that one works through {@code IFluidHandler}'s
 * slot API, and expressing a network as fake tanks makes {@code getFreeSpaceInsideTank} guess --
 * {@code getTankCapacity(int)} takes no fluid, so a synthetic empty tank cannot say how much room
 * there is for a <i>particular</i> fluid. Implementing the interface directly lets every question be
 * answered by asking the network about the fluid actually being asked about.</p>
 *
 * <p>Amounts are millibuckets throughout, matching NeoForge. Both networks agree: AE2 defines
 * {@code AEFluidKey.AMOUNT_BUCKET = 1000} and Refined Storage stores fluids in the same unit its
 * {@code IFluidHandler} integration uses.</p>
 */
public abstract class NetworkTankUtil implements ITankUtil {

    /**
     * Probe size for "how much of this fluid would you take?". Nothing is committed, so asking for
     * the maximum is free and avoids a second round trip to narrow the answer down.
     */
    private static final int UNLIMITED = Integer.MAX_VALUE;

    /**
     * Every fluid currently stored, amounts already clamped into int range.
     */
    protected abstract Stream<FluidStack> storedFluids();

    /**
     * @return how much of {@code stack} the network accepted, or would accept when simulating.
     */
    protected abstract int insert(FluidStack stack, boolean simulate);

    /**
     * @return how much of {@code stack} the network gave up, or would give up when simulating.
     */
    protected abstract int extract(FluidStack stack, boolean simulate);

    /**
     * @return how much of that fluid the network holds.
     */
    protected abstract long storedAmount(FluidStack probe);

    /**
     * Always true. A network that could not be talked to never gets this far -- the handler returns
     * no util at all in that case -- and reporting false here would make pipes refuse to connect:
     * {@code FluidRoutedPipe#canPipeConnect} gates on exactly this.
     */
    @Override
    public boolean containsTanks() {
        return true;
    }

    @Override
    public int fill(FluidIdentifierStack stack, boolean doFill) {
        return insert(stack.makeFluidStack(), !doFill);
    }

    @Override
    public @Nullable FluidIdentifierStack drain(FluidIdentifierStack stack, boolean doDrain) {
        FluidStack wanted = stack.makeFluidStack();
        int drained = extract(wanted, !doDrain);
        if (drained <= 0) {
            return null;
        }
        return stack.getFluid().makeFluidIdentifierStack(drained);
    }

    /**
     * Drains any fluid, up to {@code amount}. A network has no "first tank", so the first fluid it
     * reports is used -- which is arbitrary but matches what draining an unspecified tank means.
     */
    @Override
    public @Nullable FluidIdentifierStack drain(int amount, boolean doDrain) {
        if (amount <= 0) {
            return null;
        }
        FluidStack first = storedFluids().findFirst().orElse(FluidStack.EMPTY);
        if (first.isEmpty()) {
            return null;
        }
        FluidStack wanted = first.copy();
        wanted.setAmount(amount);
        int drained = extract(wanted, !doDrain);
        if (drained <= 0) {
            return null;
        }
        FluidIdentifier ident = FluidIdentifier.get(first);
        return ident == null ? null : ident.makeFluidIdentifierStack(drained);
    }

    @Override
    public Stream<FluidStack> tanks() {
        return storedFluids();
    }

    @Override
    public boolean canDrain(FluidIdentifier fluid) {
        return storedAmount(fluid.makeFluidStack(1)) > 0;
    }

    @Override
    public int getFreeSpaceInsideTank(FluidIdentifier type) {
        return insert(type.makeFluidStack(NetworkTankUtil.UNLIMITED), true);
    }
}
