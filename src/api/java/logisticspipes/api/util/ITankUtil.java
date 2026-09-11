package logisticspipes.api.util;

import java.util.stream.Stream;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import org.jspecify.annotations.Nullable;

import logisticspipes.api.provider.ISpecialTankUtilProvider;

/**
 * How a pipe reaches a neighbour's fluid storage.
 *
 * <p>Logistics Pipes builds one of these over the block's fluid handler capability by itself. Supply
 * your own through {@link ISpecialTankUtilProvider} only for storage that the capability cannot
 * express -- a network, where there are no tanks to enumerate.
 *
 * <p>Every method acts on its own: none of them joins a transaction opened by the caller.
 */
public interface ITankUtil {

    /** Whether there is any fluid storage here at all. Pipes refuse to connect when there is not. */
    boolean containsTanks();

    /** @return how much of {@code stack} was accepted, or would be with {@code doFill} false. */
    int fill(FluidStack stack, boolean doFill);

    /** @return what was taken out, or null if nothing of that fluid was. */
    @Nullable
    FluidStack drain(FluidStack stack, boolean doDrain);

    /**
     * Takes out up to {@code amount} of whichever fluid comes first, for a caller that does not care
     * which. Storage holding several fluids picks arbitrarily among them.
     *
     * @return what was taken out, or null if nothing was.
     */
    @Nullable
    FluidStack drain(int amount, boolean doDrain);

    /** The contents of each non-empty tank. */
    Stream<FluidStack> tanks();

    /** Whether any of that fluid could be taken out at all. */
    boolean canDrain(FluidResource fluid);

    /** How much more of {@code type} would fit, across every tank that would take it. */
    int getFreeSpaceInsideTank(FluidResource type);
}
