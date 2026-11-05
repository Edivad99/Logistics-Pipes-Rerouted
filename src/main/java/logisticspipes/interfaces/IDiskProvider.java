package logisticspipes.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import logisticspipes.utils.gui.ItemDisplay;

/**
 * A screen with a disk slot, which stores the player's request macros.
 */
public interface IDiskProvider {

    /**
     * @return the disk in the slot, or an empty stack
     */
    ItemStack getDisk();

    BlockPos getBlockPos();

    @Nullable ItemDisplay getItemDisplay();
}
