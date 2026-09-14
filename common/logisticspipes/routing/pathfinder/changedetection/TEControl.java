package logisticspipes.routing.pathfinder.changedetection;

import java.util.ArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.ticks.LPTickHandler;
import logisticspipes.ticks.QueuedTasks;

public class TEControl {

    /**
     * Called from {@link LogisticsTileGenericPipe#onLoad()}.
     * <p>
     * Non-LP neighbour changes are handled by BlockChangeListener which
     * listens for BlockEvent.EntityPlaceEvent / BlockEvent.BreakEvent.
     */
    public static void validate(final LogisticsTileGenericPipe tile) {
        final Level level = tile.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        final BlockPos pos = tile.getBlockPos();
        if (pos.getX() == 0 && pos.getY() <= 0 && pos.getZ() == 0) {
            return;
        }

        tile.activateChangeDetection();
        if (LPTickHandler.getWorldInfo(level).getWorldTick() < 5) {
            return;
        }
        QueuedTasks.queueTask(() -> {
            for (Direction dir : Direction.values()) {
                BlockPos newPos = pos.relative(dir);
                if (level.isLoaded(newPos) && level.isEmptyBlock(newPos)) {
                    continue;
                }
                BlockEntity nextTile = level.getBlockEntity(newPos);
                if (nextTile instanceof LogisticsTileGenericPipe nextPipe && nextPipe.isChangeDetectionActive()) {
                    if (SimpleServiceLocator.pipeInformationManager.isItemPipe(nextTile)) {
                        SimpleServiceLocator.pipeInformationManager.getInformationProviderFor(nextTile)
                            .refreshTileCacheOnSide(dir.getOpposite());
                    }
                    if (SimpleServiceLocator.pipeInformationManager.isItemPipe(tile)) {
                        SimpleServiceLocator.pipeInformationManager.getInformationProviderFor(tile)
                            .refreshTileCacheOnSide(dir);
                        SimpleServiceLocator.pipeInformationManager.getInformationProviderFor(tile)
                            .refreshTileCacheOnSide(dir.getOpposite());
                    }
                    for (ITileEntityChangeListener listener : new ArrayList<>(nextPipe.changeListeners)) {
                        listener.pipeAdded(pos, dir.getOpposite());
                    }
                }
            }
            return null;
        });
    }

    /**
     * Called from {@link LogisticsTileGenericPipe#setRemoved()}.
     * <p>
     * Non-LP neighbours: covered by LogisticsEventListener.onNeighborNotify (BlockEvent.NeighborNotifyEvent)
     * which flags adjacent routers for recheck when any block changes.
     */
    public static void invalidate(final LogisticsTileGenericPipe tile) {
        final Level level = tile.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        if (tile.isRoutingPipe() || !tile.isChangeDetectionActive()) {
            return;
        }
        QueuedTasks.queueTask(() -> {
            BlockPos pos = tile.getBlockPos();
            for (Direction dir : Direction.values()) {
                BlockPos newPos = pos.relative(dir);
                if (level.isLoaded(newPos) && level.isEmptyBlock(newPos)) {
                    continue;
                }
                BlockEntity nextTile = level.getBlockEntity(newPos);
                if (nextTile instanceof LogisticsTileGenericPipe nextPipe && nextPipe.isChangeDetectionActive()) {
                    if (SimpleServiceLocator.pipeInformationManager.isItemPipe(nextTile)) {
                        SimpleServiceLocator.pipeInformationManager.getInformationProviderFor(nextTile)
                            .refreshTileCacheOnSide(dir.getOpposite());
                    }
                }
            }
            for (ITileEntityChangeListener listener : new ArrayList<>(tile.changeListeners)) {
                listener.pipeRemoved(pos);
            }
            return null;
        });
    }
}
