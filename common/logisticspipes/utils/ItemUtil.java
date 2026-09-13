package logisticspipes.utils;

import net.minecraft.world.item.ItemStack;

import logisticspipes.inventory.IItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

public final class ItemUtil {

    private ItemUtil() {
    }

    /**
     * Full identity comparison: item plus every data component. Used as a re-validation guard by the
     * async modules, which plan against a snapshot and must confirm the slot did not change before
     * acting on it, so this deliberately is the strictest possible match.
     */
    public static boolean equalsWithNBT(ItemIdentifier itemid, ItemStack stack) {
        return itemid.item == stack.getItem() && itemid.components.equals(stack.getComponentsPatch());
    }

    /** Whether any slot of the inventory holds that item, whatever data is written on it. */
    public static boolean anyMatchesWithoutNBT(IItemIdentifierInventory inventory, ItemStack stack) {
        for (int slot = 0; slot < inventory.getContainerSize(); ++slot) {
            ItemIdentifierStack inSlot = inventory.getIDStackInSlot(slot);
            if (inSlot != null && inSlot.getItem().equalsWithoutNBT(stack)) {
                return true;
            }
        }
        return false;
    }

    public static int getExtractionMax(int stackCount, int maxExtractionCount, SinkReply sinkReply) {
        int max = Math.min(stackCount, maxExtractionCount);
        return sinkReply.maxNumberOfItems > 0 ? Math.min(max, sinkReply.maxNumberOfItems) : max;
    }
}
