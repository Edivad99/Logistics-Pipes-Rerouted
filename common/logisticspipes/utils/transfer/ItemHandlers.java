package logisticspipes.utils.transfer;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The two slot operations LP needs that the transfer API does not already spell out.
 *
 * <p>{@code ItemUtil} covers reading a slot and inserting into one with remainder semantics -- the
 * shape LP inherited from {@code IItemHandler}. Extraction and the slot limit have no such helper,
 * so they live here rather than being open-coded at a dozen call sites.</p>
 */
public final class ItemHandlers {

    private ItemHandlers() {
    }

    /**
     * Takes up to {@code amount} out of one slot, whatever it holds.
     *
     * <p>Replaces {@code IItemHandler#extractItem(slot, amount, simulate)}. The new API is
     * resource-addressed -- you say what you want, not just how much -- so the slot's own contents
     * are read first and used as the request.</p>
     */
    public static ItemStack extractItem(ResourceHandler<ItemResource> handler, int index, int amount,
        boolean simulate) {
        return extractItem(handler, index, amount, simulate, null);
    }

    /**
     * As {@link #extractItem(ResourceHandler, int, int, boolean)}, joining an enclosing transaction.
     *
     * <p>Same shape as {@code ItemUtil.insertItemReturnRemaining}, which this pairs with at most
     * call sites: a null context opens a root transaction, anything else nests inside it so the
     * caller's abort still undoes the extraction.</p>
     */
    public static ItemStack extractItem(ResourceHandler<ItemResource> handler, int index, int amount,
        boolean simulate, @Nullable TransactionContext parent) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemResource held = handler.getResource(index);
        if (held.isEmpty()) {
            return ItemStack.EMPTY;
        }
        try (Transaction transaction = Transaction.open(parent)) {
            int extracted = handler.extract(index, held, amount, transaction);
            if (!simulate) {
                transaction.commit();
            }
            return extracted == 0 ? ItemStack.EMPTY : held.toStack(extracted);
        }
    }

    /** Replaces {@code IItemHandler#getSlotLimit(slot)}. */
    public static int slotLimit(ResourceHandler<ItemResource> handler, int index) {
        ItemResource held = handler.getResource(index);
        return handler.getCapacityAsInt(index, held);
    }
}
