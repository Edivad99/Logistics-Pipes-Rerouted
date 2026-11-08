package logisticspipes.integrations.buildcraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.factory.tile.TileAutoWorkbenchBase;
import buildcraft.lib.tile.item.ItemHandlerSimple;
import org.jspecify.annotations.Nullable;

/**
 * What Logistics Pipes reads from a BuildCraft Auto Workbench.
 *
 * <p>BuildCraft's public API has no access to the workbench, so this reads its internal block
 * entity. Every call into it goes through {@link #read}, which {@link InternalAccess} guards.
 *
 * <p>The workbench itself is recognised by its block entity type, which does not depend on
 * BuildCraft's classes and so keeps working after that.
 */
final class AutoWorkbench {

    private static final Identifier TYPE =
        Identifier.fromNamespaceAndPath("buildcraftfactory", "entity_autoworkbench_item");

    private static final InternalAccess ACCESS = new InternalAccess("the Auto Workbench");

    private AutoWorkbench() {
    }

    static boolean is(BlockEntity blockEntity) {
        return TYPE.equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()));
    }

    /**
     * Whether {@code blockEntity} is a workbench whose internals can still be read.
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

    static int slotSize(BlockEntity blockEntity) {
        return ((TileAutoWorkbenchBase) blockEntity).getWorkbenchCrafting().getSlotSize();
    }

    static ItemStack result(BlockEntity blockEntity) {
        return ((TileAutoWorkbenchBase) blockEntity).getCurrentRecipeOutput();
    }

    static List<ItemStack> blueprint(BlockEntity blockEntity) {
        ItemHandlerSimple handler = ((TileAutoWorkbenchBase) blockEntity).getInvBlueprint();
        List<ItemStack> stacks = new ArrayList<>(handler.getSlots());
        for (int i = 0; i < handler.getSlots(); i++) {
            stacks.add(handler.getStackInSlot(i));
        }
        return stacks;
    }

    /**
     * How much of the power one craft takes the workbench has stored, from 0 to 1.
     */
    static double progress(BlockEntity blockEntity) {
        return ((TileAutoWorkbenchBase) blockEntity).getProgress(1);
    }
}
