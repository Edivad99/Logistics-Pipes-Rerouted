package logisticspipes.integrations.buildcraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.lib.internal.recipes.IngredientStack;
import buildcraft.silicon.EnumAssemblyRecipeState;
import buildcraft.silicon.tile.TileAssemblyTable;
import buildcraft.silicon.tile.TileAssemblyTable.AssemblyInstruction;
import org.jspecify.annotations.Nullable;

/**
 * What Logistics Pipes reads from a BuildCraft Assembly Table.
 *
 * <p>The recipes saved in a table are only kept in its internal block entity. Every call into it goes
 * through {@link #read}, which {@link InternalAccess} guards.
 *
 * <p>The table itself is recognised by its block entity type, which does not depend on BuildCraft's
 * classes and so keeps working after that.
 */
final class AssemblyTable {

    private static final Identifier TYPE =
        Identifier.fromNamespaceAndPath("buildcraftsilicon", "entity_assembly_table");

    private static final InternalAccess ACCESS = new InternalAccess("the Assembly Table");

    private AssemblyTable() {
    }

    /**
     * One ingredient of an assembly recipe, and how many of it the recipe takes.
     */
    record Requirement(Ingredient ingredient, int count) {}

    /**
     * An assembly recipe, as the crafting pipe needs it.
     */
    record Recipe(ItemStack output, List<Requirement> requirements) {}

    static boolean is(BlockEntity blockEntity) {
        return TYPE.equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()));
    }

    /**
     * Whether {@code blockEntity} is a table whose internals can still be read.
     */
    static boolean isReadable(BlockEntity blockEntity) {
        return !ACCESS.isDisabled() && is(blockEntity);
    }

    /**
     * Runs a read of BuildCraft's internals.
     *
     * @return what it read, or null when the internals no longer match and reading is now turned off
     */
    static <T> @Nullable T read(Supplier<T> reader) {
        return ACCESS.read(reader);
    }

    /**
     * The recipe the table is working on, or else the first one saved in it.
     *
     * <p>A table can hold several saved recipes and works through them in turn, while a crafting pipe
     * has room for one: the one it is on right now is the best guess at what the player means.
     */
    static Optional<Recipe> selectedRecipe(BlockEntity blockEntity) {
        AssemblyInstruction selected = null;
        for (Map.Entry<AssemblyInstruction, EnumAssemblyRecipeState> entry : ((TileAssemblyTable) blockEntity).recipesStates
            .entrySet()) {
            EnumAssemblyRecipeState state = entry.getValue();
            if (state == EnumAssemblyRecipeState.SAVED_ENOUGH_ACTIVE) {
                selected = entry.getKey();
                break;
            }
            if (selected == null && state != EnumAssemblyRecipeState.POSSIBLE) {
                selected = entry.getKey();
            }
        }
        if (selected == null) {
            return Optional.empty();
        }
        List<Requirement> requirements = new ArrayList<>();
        for (IngredientStack stack : selected.recipe.getInputsFor(selected.output)) {
            requirements.add(new Requirement(stack.ingredient, stack.count));
        }
        return Optional.of(new Recipe(selected.output.copy(), requirements));
    }
}
