package logisticspipes.integrations.buildcraft;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.fml.ModList;

import buildcraft.api.v2.BuildCraftApi;
import buildcraft.api.v2.BuildCraftServices;
import org.jspecify.annotations.Nullable;

import logisticspipes.LPConstants;

/**
 * Recognises BuildCraft pipes, so a pipe connected to one can draw the arm that fits it.
 *
 * <p>Asks BuildCraft's public API. Safe to call without BuildCraft installed: its classes are only
 * touched once it is known to be loaded.
 */
public final class BuildCraftPipes {

    private static final boolean LOADED = ModList.get().isLoaded(LPConstants.BUILDCRAFT_TRANSPORT_MOD_ID);

    private BuildCraftPipes() {
    }

    /**
     * Whether {@code blockEntity} holds a BuildCraft pipe.
     */
    public static boolean isPipe(@Nullable BlockEntity blockEntity) {
        if (!LOADED || blockEntity == null) {
            return false;
        }
        Level level = blockEntity.getLevel();
        return level != null && Loaded.isPipe(level, blockEntity);
    }

    /**
     * Kept apart so that BuildCraft's classes are only resolved when it is installed.
     */
    private static final class Loaded {

        private static boolean isPipe(Level level, BlockEntity blockEntity) {
            return BuildCraftApi.service(BuildCraftServices.PIPES).pipe(level, blockEntity.getBlockPos()).isPresent();
        }
    }
}
