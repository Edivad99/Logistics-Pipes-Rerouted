package logisticspipes.util;

import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.common.Tags;

import logisticspipes.world.item.LPItems;

public final class PipeConfigTools {

    private PipeConfigTools() {
    }

    /**
     * Whether right-clicking a pipe with this item opens or configures it.
     */
    public static boolean canConfigure(ItemStack stack) {
        return stack.is(LPItems.PIPE_MANAGER) || stack.is(Tags.Items.TOOLS_WRENCH);
    }
}
