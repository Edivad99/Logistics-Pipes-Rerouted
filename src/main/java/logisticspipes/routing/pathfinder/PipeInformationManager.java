package logisticspipes.routing.pathfinder;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jspecify.annotations.Nullable;

public final class PipeInformationManager {

    private PipeInformationManager() {
    }

    public static @Nullable IPipeInformationProvider getInformationProviderFor(@Nullable BlockEntity tile) {
        return switch (tile) {
            case IPipeInformationProvider provider -> provider;
            case ISubMultiBlockPipeInformationProvider subBlock -> subBlock.getMainTile();
            case null, default -> null;
        };
    }

    public static boolean canConnect(IPipeInformationProvider startPipe, IPipeInformationProvider provider,
        Direction direction, boolean flag) {
        return startPipe.canConnect(provider.getTile(), direction, flag) && provider.canConnect(startPipe.getTile(),
            direction.getOpposite(), flag);
    }

    /**
     * Whether two neighbouring block entities would form a pipe connection through {@code way}.
     *
     * <p>True when neither side objects: a non-pipe on one end is not a refusal, it just has no
     * opinion, which is what lets a pipe connect to a plain inventory.
     */
    public static boolean canConnect(@Nullable BlockEntity from, @Nullable BlockEntity to, Direction way) {
        return canConnect(from, to, way, false);
    }

    public static boolean canConnect(@Nullable BlockEntity from, @Nullable BlockEntity to, Direction way,
        boolean ignoreSystemDisconnection) {
        if (from == null || to == null) {
            return false;
        }
        IPipeInformationProvider fromInfo = getInformationProviderFor(from);
        IPipeInformationProvider toInfo = getInformationProviderFor(to);
        if (fromInfo == null && toInfo == null) {
            return false;
        }
        if (fromInfo != null && !fromInfo.canConnect(to, way, ignoreSystemDisconnection)) {
            return false;
        }
        return toInfo == null || toInfo.canConnect(from, way.getOpposite(), ignoreSystemDisconnection);
    }

    /**
     * Whether {@code tile} is a pipe itself; the other blocks of a multiblock pipe are not.
     */
    public static boolean isPipe(@Nullable BlockEntity tile) {
        return tile instanceof IPipeInformationProvider;
    }

    /**
     * Whether {@code tile} is no part of a pipe, not even of a multiblock one.
     */
    public static boolean isNotAPipe(BlockEntity tile) {
        return !(tile instanceof IPipeInformationProvider || tile instanceof ISubMultiBlockPipeInformationProvider);
    }
}
