package logisticspipes.routing.pathfinder.changedetection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import logisticspipes.asm.te.ILPTEInformation;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.ticks.QueuedTasks;
import logisticspipes.util.CoordinateUtils;
import logisticspipes.util.DoubleCoordinates;

/**
 * Listens for block-place and block-break events so LP pipes can refresh their
 * tile cache when a non-LP neighbour appears or disappears.
 *
 * Previously handled globally via ASM injection into all TileEntities; now done
 * via NeoForge block events registered on {@code MinecraftForge.EVENT_BUS}.
 *
 * <p>Both events fire BEFORE the change is committed, so notifications are
 * queued via {@link QueuedTasks} to run on the next server tick when the new
 * block state is already in place.</p>
 */
public class BlockChangeListener {

    /**
     * Fired before a block is placed.  Queue a cache refresh so adjacent LP
     * pipes pick up the new neighbour on the next tick.
     */
    @SubscribeEvent
    public void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level)) return;
        if (!MainProxy.isServer(level)) return;
        final BlockPos pos = event.getPos();
        QueuedTasks.queueTask(() -> {
            notifyAdjacentPipes(level, pos);
            return null;
        });
    }

    /**
     * Fired before a block is broken.  Queue a cache refresh so adjacent LP
     * pipes notice the neighbour is gone on the next tick.
     */
    @SubscribeEvent
    public void onBlockBroken(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof Level level)) return;
        if (!MainProxy.isServer(level)) return;
        final BlockPos pos = event.getPos();
        QueuedTasks.queueTask(() -> {
            notifyAdjacentPipes(level, pos);
            return null;
        });
    }

    /**
     * For each adjacent position to {@code changedPos}, if there is an LP item
     * pipe, tell it to refresh the side that faces {@code changedPos}.
     */
    private static void notifyAdjacentPipes(Level level, BlockPos changedPos) {
        DoubleCoordinates changed = new DoubleCoordinates(
                changedPos.getX(), changedPos.getY(), changedPos.getZ());

        for (Direction dir : Direction.values()) {
            DoubleCoordinates adjacent = CoordinateUtils.sum(changed, dir);
            if (!adjacent.blockExists(level)) continue;

            BlockEntity adjacentTE = adjacent.getTileEntity(level);
            if (adjacentTE == null) continue;

            // Guard: only LP-managed TEs carry ILPTEInformation
            if (!(adjacentTE instanceof ILPTEInformation lpInfo)) continue;
            if (lpInfo.getLPTileEntityObject() == null) continue;

            if (SimpleServiceLocator.pipeInformationManager.isItemPipe(adjacentTE)) {
                // dir goes from changed → adjacent; from adjacent's perspective
                // the changed block is in direction dir.getOpposite().
                SimpleServiceLocator.pipeInformationManager
                        .getInformationProviderFor(adjacentTE)
                        .refreshTileCacheOnSide(dir.getOpposite());
            }
        }
    }
}
