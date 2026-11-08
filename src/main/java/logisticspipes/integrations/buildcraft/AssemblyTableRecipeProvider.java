package logisticspipes.integrations.buildcraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.api.property.BitSetProperty;
import logisticspipes.crafting.ICraftingRecipeProvider;
import logisticspipes.inventory.IItemIdentifierInventory;
import logisticspipes.inventory.SlotAccess;

/**
 * Lets a crafting pipe import the recipe of a BuildCraft Assembly Table next to it, and open it.
 *
 * <p>The ingredients keep their counts, since an assembly recipe takes more than one of some. An
 * ingredient that accepts several items, such as a tag, is imported as the first of them.
 */
public class AssemblyTableRecipeProvider implements ICraftingRecipeProvider {

    private static final int INPUT_SLOTS = 9;
    private static final int RESULT_SLOT = 9;
    /**
     * The most one slot of the crafting pipe's recipe holds.
     */
    private static final int SLOT_LIMIT = 127;

    @Override
    public boolean canOpenScreen(BlockEntity blockEntity) {
        return AssemblyTable.is(blockEntity);
    }

    /**
     * Sends what BuildCraft's own right click sends along: the table menu reads its position from the
     * open packet.
     */
    @Override
    public boolean openScreen(ServerPlayer player, BlockEntity blockEntity) {
        if (!AssemblyTable.is(blockEntity) || !(blockEntity instanceof MenuProvider menuProvider)) {
            return false;
        }
        BlockPos pos = blockEntity.getBlockPos();
        return player.openMenu(menuProvider, buf -> buf.writeBlockPos(pos)).isPresent();
    }

    @Override
    public boolean importRecipe(BlockEntity blockEntity, IItemIdentifierInventory inventory) {
        if (!AssemblyTable.isReadable(blockEntity)) {
            return false;
        }
        Optional<AssemblyTable.Recipe> recipe = AssemblyTable.read(() -> AssemblyTable.selectedRecipe(blockEntity));
        if (recipe == null || recipe.isEmpty() || recipe.get().output().isEmpty()) {
            return false;
        }

        List<ItemStack> inputs = new ArrayList<>();
        for (AssemblyTable.Requirement requirement : recipe.get().requirements()) {
            Optional<Holder<Item>> item = requirement.ingredient().items().findFirst();
            if (item.isEmpty()) {
                return false;
            }
            for (int left = requirement.count(); left > 0; left -= SLOT_LIMIT) {
                inputs.add(new ItemStack(item.get(), Math.min(left, SLOT_LIMIT)));
            }
        }
        if (inputs.size() > INPUT_SLOTS) {
            return false;
        }

        for (int i = 0; i < INPUT_SLOTS; i++) {
            inventory.setItem(i, i < inputs.size() ? inputs.get(i) : ItemStack.EMPTY);
        }
        inventory.setItem(RESULT_SLOT, recipe.get().output());
        return true;
    }

    @Override
    public void importFuzzyFlags(BlockEntity blockEntity, SlotAccess slotAccess, BitSetProperty fuzzyFlags) {
        // Each ingredient is imported as one item, so there are no fuzzy flags to import.
    }
}
