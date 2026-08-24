package logisticspipes.utils.transfer;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import org.jspecify.annotations.Nullable;

/**
 * A single-tank fluid store: what {@code FluidTank} was, on the API that replaced it.
 *
 * <p>NeoForge's {@code FluidTank} is deprecated for removal along with the rest of
 * {@code IFluidHandler}. Its successor is {@link FluidStacksResourceHandler}, which this extends
 * with a size of one -- so the tank *is* the capability and needs no wrapper, and it inherits both
 * the transaction handling and the {@code ValueIOSerializable} round trip.</p>
 *
 * <p>What it adds back is ergonomics. LP's pipe logic fills and drains tanks in dozens of places
 * that have no transaction of their own, so the methods below wrap each operation in one. They take
 * the enclosing context as an argument, in the shape {@code ItemUtil} uses: null opens a root
 * transaction, which is what LP's tick logic wants, and a caller that is already inside a
 * transaction passes it so its own abort still undoes the write.</p>
 *
 * <p>Note that the capability itself never comes through here. A neighbour calling
 * {@code insert}/{@code extract} lands on the inherited {@link FluidStacksResourceHandler} methods
 * with its own transaction; these wrappers exist only for LP's internal callers.</p>
 */
public class LPFluidTank extends FluidStacksResourceHandler {

    public LPFluidTank(int capacity) {
        super(1, capacity);
    }

    public FluidStack getFluid() {
        return FluidUtil.getStack(this, 0);
    }

    public void setFluid(FluidStack stack) {
        set(0, FluidResource.of(stack), stack.getAmount());
    }

    public int getFluidAmount() {
        return getAmountAsInt(0);
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isEmpty() {
        return getFluid().isEmpty();
    }

    public int getSpace() {
        return Math.max(0, getCapacity() - getFluidAmount());
    }

    /** @return how much was accepted */
    public int fill(FluidStack resource, boolean execute) {
        return fill(resource, execute, null);
    }

    /** @return how much was accepted */
    public int fill(FluidStack resource, boolean execute, @Nullable TransactionContext parent) {
        if (resource.isEmpty()) {
            return 0;
        }
        try (Transaction transaction = Transaction.open(parent)) {
            int filled = insert(0, FluidResource.of(resource), resource.getAmount(), transaction);
            if (execute) {
                transaction.commit();
            }
            return filled;
        }
    }

    /** @return what was actually removed, empty if nothing */
    public FluidStack drain(int maxDrain, boolean execute) {
        return drain(maxDrain, execute, null);
    }

    /** @return what was actually removed, empty if nothing */
    public FluidStack drain(int maxDrain, boolean execute, @Nullable TransactionContext parent) {
        FluidStack contained = getFluid();
        if (contained.isEmpty() || maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        try (Transaction transaction = Transaction.open(parent)) {
            int drained = extract(0, FluidResource.of(contained), maxDrain, transaction);
            if (execute) {
                transaction.commit();
            }
            return drained == 0 ? FluidStack.EMPTY : contained.copyWithAmount(drained);
        }
    }

    /** As {@link #drain(int, boolean)}, but only if the tank holds this exact fluid. */
    public FluidStack drain(FluidStack resource, boolean execute) {
        if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(getFluid(), resource)) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), execute);
    }
}
