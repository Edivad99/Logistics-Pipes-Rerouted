package logisticspipes.inventory.transactor;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;

import org.jspecify.annotations.Nullable;

public class TransactorSimple extends Transactor {

    protected ResourceHandler<ItemResource> inventory;

    public TransactorSimple(ResourceHandler<ItemResource> inventory) {
        this.inventory = inventory;
    }

    /**
     * Tops up the stacks already in the inventory before starting new ones. {@code orientation} is
     * unused: the handler was already looked up for the side being inserted into.
     */
    @Override
    public int inject(ItemStack stack, @Nullable Direction orientation, boolean doAdd) {
        List<Integer> filledSlots = new ArrayList<>(inventory.size());
        List<Integer> emptySlots = new ArrayList<>(inventory.size());
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (canPutStackInSlot(slot, stack)) {
                if (ItemUtil.getStack(inventory, slot).isEmpty()) {
                    emptySlots.add(slot);
                } else {
                    filledSlots.add(slot);
                }
            }
        }

        int injected = 0;
        injected = tryPut(stack, filledSlots, injected, doAdd);
        injected = tryPut(stack, emptySlots, injected, doAdd);

        return injected;
    }

    private int tryPut(ItemStack stack, List<Integer> slots, int injected, boolean doAdd) {
        int realInjected = injected;

        if (realInjected >= stack.getCount()) {
            return realInjected;
        }

        for (int slot : slots) {
            ItemStack stackInSlot = ItemUtil.getStack(inventory, slot);
            if (stackInSlot.isEmpty() || ItemStack.isSameItemSameComponents(stackInSlot, stack)) {
                int used = addToSlot(slot, stack, realInjected, doAdd);
                if (used > 0) {
                    realInjected += used;
                    if (realInjected >= stack.getCount()) {
                        return realInjected;
                    }
                }
            }
        }

        return realInjected;
    }

    /**
     * Moves what is left of {@code stack}, past the {@code injected} items already placed, into
     * {@code slot}.
     *
     * @return the number of items moved.
     */
    protected int addToSlot(int slot, ItemStack stack, int injected, boolean doAdd) {
        int available = stack.getCount() - injected;

        ItemStack newStack = stack.copy();
        newStack.setCount(available);

        ItemStack rest = ItemUtil.insertItemReturnRemaining(inventory, slot, newStack, !doAdd, null);
        return available - rest.getCount();
    }

    private boolean canPutStackInSlot(int slot, ItemStack stack) {
        ItemStack toTest = stack.copyWithCount(1);
        return ItemUtil.insertItemReturnRemaining(inventory, slot, toTest, true, null).isEmpty();
    }
}
