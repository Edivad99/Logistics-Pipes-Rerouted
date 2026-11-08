package logisticspipes.integrations.buildcraft;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.api.property.BitSetProperty;
import logisticspipes.crafting.ICraftingRecipeProvider;
import logisticspipes.inventory.IItemIdentifierInventory;
import logisticspipes.inventory.SlotAccess;

/**
 * Lets a crafting pipe import the recipe of a BuildCraft Auto Workbench next to it, and open it.
 *
 * <p>It keeps claiming the workbench even once {@link AutoWorkbench} has turned reading off, so the
 * crafting pipe never falls back to opening it the generic way, which crashes the client.
 */
public class AutoWorkbenchRecipeProvider implements ICraftingRecipeProvider {

    @Override
    public boolean canOpenScreen(BlockEntity blockEntity) {
        return AutoWorkbench.is(blockEntity);
    }

    /**
     * Sends what BuildCraft's own right click sends along: the workbench menu reads the slot size and
     * the position from the open packet, and crashes the client without them.
     */
    @Override
    public boolean openScreen(ServerPlayer player, BlockEntity blockEntity) {
        if (!AutoWorkbench.isReadable(blockEntity) || !(blockEntity instanceof MenuProvider menuProvider)) {
            return false;
        }
        Integer slotSize = AutoWorkbench.read(() -> AutoWorkbench.slotSize(blockEntity));
        if (slotSize == null) {
            return false;
        }
        BlockPos pos = blockEntity.getBlockPos();
        return player.openMenu(menuProvider, buf -> {
            buf.writeInt(slotSize);
            buf.writeBlockPos(pos);
        }).isPresent();
    }

    @Override
    public boolean importRecipe(BlockEntity blockEntity, IItemIdentifierInventory inventory) {
        if (!AutoWorkbench.isReadable(blockEntity)) {
            return false;
        }
        ItemStack result = AutoWorkbench.read(() -> AutoWorkbench.result(blockEntity));
        List<ItemStack> blueprint = AutoWorkbench.read(() -> AutoWorkbench.blueprint(blockEntity));
        if (result == null || blueprint == null || result.isEmpty()) {
            return false;
        }

        inventory.setItem(9, result.copy());
        for (int i = 0; i < blueprint.size() && i < 9; i++) {
            inventory.setItem(i, blueprint.get(i).copyWithCount(1));
        }
        inventory.getSlotAccess().compactFirst(9);
        return true;
    }

    @Override
    public void importFuzzyFlags(BlockEntity blockEntity, SlotAccess slotAccess, BitSetProperty fuzzyFlags) {
        // The workbench matches its ingredients exactly, so there are no fuzzy flags to import.
    }
}
