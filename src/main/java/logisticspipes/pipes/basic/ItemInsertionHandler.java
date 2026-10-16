package logisticspipes.pipes.basic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.transfer.VoidingResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import logisticspipes.world.level.block.LogisticsGenericPipeBlock;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;

/**
 * The item capability a pipe exposes to its neighbors: a drop slot that swallows items into the
 * logistics network. Nothing is ever stored here and nothing can be taken out.
 *
 * <p>That description is {@link VoidingResourceHandler} almost exactly -- one index, accepts
 * anything, holds nothing, gives nothing back -- so the only thing left to write is the part that
 * makes the items go somewhere.</p>
 *
 * <p>An insert only records the items. They enter the pipe when the root transaction commits, and
 * an aborted transaction -- a simulation -- leaves no trace. Pushing them in from {@link #insert}
 * itself is not an option: routing an item asks inventories for room, which opens root
 * transactions of its own, and a simulated insert would put real items in the pipe.</p>
 *
 * <p>The price is that the count is promised before the transport has been asked. Whatever the
 * pipe still refuses on commit -- it has nowhere to send it -- is dropped next to it.</p>
 */
public class ItemInsertionHandler extends VoidingResourceHandler<ItemResource> {

    private final LogisticsGenericPipeBlockEntity pipe;
    private final Direction dir;
    private final PendingItems pending = new PendingItems();

    public ItemInsertionHandler(LogisticsGenericPipeBlockEntity pipe, Direction dir) {
        super(ItemResource.EMPTY);
        this.pipe = pipe;
        this.dir = dir;
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (index != 0 || resource.isEmpty() || amount <= 0 || !canAccept()) {
            return 0;
        }
        pending.updateSnapshots(transaction);
        pending.stacks.add(resource.toStack(amount));
        return amount;
    }

    /**
     * The checks {@code LogisticsGenericPipeBlockEntity#injectItem} makes before it takes anything.
     */
    private boolean canAccept() {
        Level level = pipe.getLevel();
        return level != null && !level.isClientSide() &&
            LogisticsGenericPipeBlock.isValid(pipe.pipe) && pipe.isPipeConnectedCached(dir);
    }

    /**
     * The items inserted in the current transaction; a snapshot is how many there were.
     */
    private class PendingItems extends SnapshotJournal<Integer> {

        private final List<ItemStack> stacks = new ArrayList<>();

        @Override
        protected Integer createSnapshot() {
            return stacks.size();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            stacks.subList(snapshot, stacks.size()).clear();
        }

        @Override
        protected void onRootCommit(Integer originalState) {
            List<ItemStack> committed = List.copyOf(stacks);
            stacks.clear();
            for (ItemStack stack : committed) {
                ItemStack leftover = canAccept() ? pipe.insertItem(dir, stack) : stack;
                Level level = pipe.getLevel();
                if (!leftover.isEmpty() && level != null) {
                    Containers.dropItemStack(level, pipe.getBlockPos().getX(), pipe.getBlockPos().getY(),
                        pipe.getBlockPos().getZ(), leftover);
                }
            }
        }
    }
}
