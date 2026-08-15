package logisticspipes.interfaces;

import java.util.stream.Stream;
import javax.annotation.Nullable;

import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.FluidIdentifierStack;
import net.neoforged.neoforge.fluids.FluidStack;

public interface ITankUtil {

	boolean containsTanks();

	int fill(FluidIdentifierStack stack, boolean doFill);

	@Nullable FluidIdentifierStack drain(FluidIdentifierStack stack, boolean doDrain);

	@Nullable FluidIdentifierStack drain(int amount, boolean doDrain);

	/** Returns the contents of each non-empty tank slot. */
	Stream<FluidStack> tanks();

	/**
	 * Type only — amount is ignored
	 */
	boolean canDrain(FluidIdentifier fluid);

	int getFreeSpaceInsideTank(FluidIdentifier type);
}
