package logisticspipes.utils;

import net.neoforged.neoforge.fluids.FluidStack;

import org.jspecify.annotations.Nullable;

import logisticspipes.utils.item.ItemIdentifierStack;

public class FluidIdentifierStack implements Comparable<FluidIdentifierStack> {

	private Object ccType;
	private final FluidIdentifier fluid;
	private int milliBuckets;

	public FluidIdentifierStack(FluidIdentifier fluid, int milliBuckets) {
		this.fluid = fluid;
		setAmount(milliBuckets);
	}

    @Nullable
	public static FluidIdentifierStack getFromStack(FluidStack stack) {
		FluidIdentifier fluid = FluidIdentifier.get(stack);
		if (fluid == null) return null;
		return new FluidIdentifierStack(fluid, stack.getAmount());
	}

    @Nullable
	public static FluidIdentifierStack getFromStack(ItemIdentifierStack stack) {
		FluidIdentifier fluid = FluidIdentifier.get(stack);
		if (fluid == null) return null;
		return new FluidIdentifierStack(fluid, stack.getStackSize());
	}

	public FluidIdentifier getFluid() {
		return fluid;
	}

	/**
	 * @return the stackSize
	 */
	public int getAmount() {
		return milliBuckets;
	}

	public void setAmount(int milliBuckets) {
		this.milliBuckets = milliBuckets;
	}

	public void lowerAmount(int milliBuckets) {
		this.milliBuckets -= milliBuckets;
	}

	public void raiseAmount(int milliBuckets) {
		this.milliBuckets += milliBuckets;
	}

	public FluidStack makeFluidStack() {
		return fluid.makeFluidStack(milliBuckets);
	}

	@Override
	public int compareTo(FluidIdentifierStack o) {
		int c = fluid.compareTo(o.fluid);
		if (c == 0) {
			return Integer.compare(getAmount(), o.getAmount());
		}
		return c;
	}
}
