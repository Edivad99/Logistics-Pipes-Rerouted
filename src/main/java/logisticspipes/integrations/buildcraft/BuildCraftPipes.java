package logisticspipes.integrations.buildcraft;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.fml.ModList;

import buildcraft.api.v2.BuildCraftApi;
import buildcraft.api.v2.BuildCraftServices;
import buildcraft.api.v2.pipe.PipeService;
import org.jspecify.annotations.Nullable;

import logisticspipes.LPConstants;
import logisticspipes.routing.pathfinder.IPipeInformationProvider;

/**
 * Recognises BuildCraft pipes: a pipe connected to one draws the arm that fits it, and an item
 * pipe joins the logistics network as a stretch of plain pipe.
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
     * How the logistics network sees {@code blockEntity} if it holds a BuildCraft item pipe: a pipe
     * that routers find each other through and that routed items cross.
     *
     * @return null for anything else, BuildCraft's fluid and power pipes included
     */
    public static @Nullable IPipeInformationProvider informationFor(@Nullable BlockEntity blockEntity) {
        if (!LOADED || blockEntity == null) {
            return null;
        }
        Level level = blockEntity.getLevel();
        return level == null ? null : Loaded.informationFor(level, blockEntity);
    }

    static PipeService service() {
        return BuildCraftApi.service(BuildCraftServices.PIPES);
    }

    /**
     * Kept apart so that BuildCraft's classes are only resolved when it is installed.
     */
    private static final class Loaded {

        private static boolean isPipe(Level level, BlockEntity blockEntity) {
            return service().pipe(level, blockEntity.getBlockPos()).isPresent();
        }

        private static @Nullable IPipeInformationProvider informationFor(Level level, BlockEntity blockEntity) {
            return service().pipe(level, blockEntity.getBlockPos())
                .filter(BuildCraftPipeInformation::carriesItems)
                .map(view -> (IPipeInformationProvider) new BuildCraftPipeInformation(blockEntity, view))
                .orElse(null);
        }
    }
}
