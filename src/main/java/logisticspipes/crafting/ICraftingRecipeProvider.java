package logisticspipes.crafting;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.api.property.BitSetProperty;
import logisticspipes.inventory.IItemIdentifierInventory;
import logisticspipes.inventory.SlotAccess;
import logisticspipes.utils.BlockMenus;

public interface ICraftingRecipeProvider {

    boolean canOpenScreen(BlockEntity blockEntity);

    /**
     * Opens the screen of a block this provider {@linkplain #canOpenScreen can open}.
     *
     * <p>The default opens it the way {@link BlockMenus#openFor} does. A block whose menu needs extra
     * data sent to the client has to override this and send it.
     *
     * @return whether a screen was opened
     */
    default boolean openScreen(ServerPlayer player, BlockEntity blockEntity) {
        return BlockMenus.openFor(player, blockEntity.getBlockPos());
    }

    boolean importRecipe(BlockEntity blockEntity, IItemIdentifierInventory inventory);

    void importFuzzyFlags(BlockEntity blockEntity, SlotAccess slotAccess, BitSetProperty fuzzyFlags);

}
