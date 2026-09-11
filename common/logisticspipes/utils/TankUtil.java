package logisticspipes.utils;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import org.jspecify.annotations.Nullable;

import logisticspipes.api.util.ITankUtil;

/**
 * LP's view of a neighbouring fluid inventory.
 *
 * <p>Was written against {@code IFluidHandler}, removed with the 21.9 transfer rework. The shape is
 * the same -- indexed tanks with a fill and a drain -- with the {@code simulate} flag replaced by a
 * transaction that is committed or dropped.</p>
 *
 * <p>Each method opens a root transaction. Every caller is LP tick logic starting a fresh
 * operation, never a participant in someone else's: {@code ITankUtil} is how a pipe reaches out to
 * a <em>neighbour's</em> tanks, so nothing upstream holds a context to join.</p>
 */
public class TankUtil implements ITankUtil {

	private final ResourceHandler<FluidResource> fluidhandler;

	public TankUtil(ResourceHandler<FluidResource> fluidhandler) {
		this.fluidhandler = fluidhandler;
	}

	@Override
	public boolean containsTanks() {
		return fluidhandler.size() > 0;
	}

	@Override
	public int fill(FluidStack toFill, boolean doFill) {
		if (toFill.isEmpty()) {
			return 0;
		}
		try (Transaction transaction = Transaction.openRoot()) {
			int filled = fluidhandler.insert(FluidResource.of(toFill), toFill.getAmount(), transaction);
			if (doFill) {
				transaction.commit();
			}
			return filled;
		}
	}

	@Override
	public @Nullable FluidStack drain(FluidStack wanted, boolean doDrain) {
		if (wanted.isEmpty()) {
			return null;
		}
		try (Transaction transaction = Transaction.openRoot()) {
			int drained = fluidhandler.extract(FluidResource.of(wanted), wanted.getAmount(), transaction);
			if (doDrain) {
				transaction.commit();
			}
			return drained == 0 ? null : wanted.copyWithAmount(drained);
		}
	}

	@Override
	public @Nullable FluidStack drain(int amount, boolean doDrain) {
		// No fluid named, so drain the first thing there is -- what the old amount-only overload of
		// IFluidHandler#drain did. The new API is always resource-addressed.
		FluidResource first = IntStream.range(0, fluidhandler.size())
				.mapToObj(fluidhandler::getResource)
				.filter(resource -> !resource.isEmpty())
				.findFirst()
				.orElse(FluidResource.EMPTY);
		if (first.isEmpty() || amount <= 0) {
			return null;
		}
		try (Transaction transaction = Transaction.openRoot()) {
			int drained = fluidhandler.extract(first, amount, transaction);
			if (doDrain) {
				transaction.commit();
			}
			return drained == 0 ? null : first.toStack(drained);
		}
	}

	@Override
	public Stream<FluidStack> tanks() {
		return IntStream.range(0, fluidhandler.size())
				.mapToObj(index -> FluidUtil.getStack(fluidhandler, index))
				.filter(stack -> !stack.isEmpty());
	}

	@Override
	public boolean canDrain(FluidResource fluid) {
		// The transaction is the simulation: extract one unit and drop it.
		try (Transaction transaction = Transaction.openRoot()) {
			return fluidhandler.extract(fluid, 1, transaction) > 0;
		}
	}

	@Override
	public int getFreeSpaceInsideTank(FluidResource type) {
		int free = 0;
		for (int i = 0; i < fluidhandler.size(); i++) {
			FluidResource content = fluidhandler.getResource(i);
			if (content.isEmpty()) {
				free += fluidhandler.getCapacityAsInt(i, type);
			} else if (content.equals(type)) {
				free += fluidhandler.getCapacityAsInt(i, content) - fluidhandler.getAmountAsInt(i);
			}
		}
		return free;
	}
}
