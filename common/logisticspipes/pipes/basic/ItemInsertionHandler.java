package logisticspipes.pipes.basic;

import java.util.ArrayList;
import java.util.List;

import logisticspipes.interfaces.ILPItemAcceptor;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.VoidingResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The item capability a pipe exposes to its neighbours: a drop slot that swallows items into the
 * logistics network. Nothing is ever stored here and nothing can be taken out.
 *
 * <p>That description is {@link VoidingResourceHandler} almost exactly -- one index, accepts
 * anything, holds nothing, gives nothing back -- so the only thing left to write is the part that
 * makes the items go somewhere.</p>
 *
 * <p><strong>An insert here is not undoable.</strong> {@link #insert} pushes the items into the
 * pipe transport immediately and returns how many were really taken; if the surrounding transaction
 * is then aborted, they stay in the pipe. This is the lesser of two evils rather than a good
 * answer. The alternative -- accept optimistically and push on commit, which is what the plain
 * voiding handler does -- means promising a count before the transport has been asked, and LP's
 * transport routinely takes less than offered when a pipe is backed up, so the difference would be
 * items destroyed on every congested insert. Doing the work up front makes the returned count
 * exact, and the price is a duplication window on the rare aborted transaction.</p>
 *
 * <p>Closing the gap properly needs a reservation in {@code PipeTransportLogistics}: something that
 * claims capacity, can be released, and turns into real traveling items on commit. Until then this
 * is no worse than 1.21.8, where {@code insertItem(simulate = false)} had exactly the same
 * one-way behaviour.</p>
 */
public class ItemInsertionHandler extends VoidingResourceHandler<ItemResource> {

	public static final List<ILPItemAcceptor> ACCEPTORS = new ArrayList<>();

	private final LogisticsTileGenericPipe pipe;
	private final Direction dir;

	public ItemInsertionHandler(LogisticsTileGenericPipe pipe, Direction dir) {
		super(ItemResource.EMPTY);
		this.pipe = pipe;
		this.dir = dir;
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		if (index != 0 || resource.isEmpty() || amount <= 0) {
			return 0;
		}
		ItemStack leftover = handleItemInsetion(pipe, dir, resource.toStack(amount));
		return amount - leftover.getCount();
	}

	public static ItemStack handleItemInsetion(LogisticsTileGenericPipe pipe, Direction from, ItemStack stack) {
		for (ILPItemAcceptor acceptor : ACCEPTORS) {
			if (acceptor.accept(pipe, from, stack)) {
				return ItemStack.EMPTY;
			}
		}
		return pipe.insertItem(from, stack);
	}
}
