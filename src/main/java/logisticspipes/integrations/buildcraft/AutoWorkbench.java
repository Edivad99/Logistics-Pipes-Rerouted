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

import logisticspipes.LogisticsPipes;

/**
 * What Logistics Pipes reads from a BuildCraft Auto Workbench.
 *
 * <p>BuildCraft's public API has no access to the workbench, so this reads its internal block
 * entity. Those internals can change in any BuildCraft release: every call into them goes through
 * {@link #read}, and the first {@link LinkageError} turns all of it off instead of crashing the game.
 *
 * <p>The workbench itself is recognised by its block entity type, which does not depend on
 * BuildCraft's classes and so keeps working after that.
 */
final class AutoWorkbench {

    private static final Identifier TYPE =
        Identifier.fromNamespaceAndPath("buildcraftfactory", "entity_autoworkbench_item");

    private static boolean disabled;

    private AutoWorkbench() {
    }

    static boolean is(BlockEntity blockEntity) {
        return TYPE.equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()));
    }

    /**
     * Whether {@code blockEntity} is a workbench whose internals can still be read.
     */
    static boolean isReadable(BlockEntity blockEntity) {
        return !disabled && is(blockEntity);
    }

    /**
     * Runs a read of BuildCraft's internals.
     *
     * @return what it read, or null when the internals no longer match and reading is now turned off
     */
    static <T> @Nullable T read(Supplier<T> reader) {
        if (disabled) {
            return null;
        }
        try {
            return reader.get();
        } catch (LinkageError e) {
            disabled = true;
            LogisticsPipes.LOG.warn("This BuildCraft version changed the Auto Workbench in a way Logistics "
                + "Pipes does not know; reading it from Logistics Pipes is turned off", e);
            return null;
        }
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
