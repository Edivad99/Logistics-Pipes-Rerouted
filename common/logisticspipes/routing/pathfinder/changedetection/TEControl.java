package logisticspipes.routing.pathfinder.changedetection;

import java.util.ArrayList;
import java.util.Objects;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.asm.te.ILPTEInformation;
import logisticspipes.asm.te.ITileEntityChangeListener;
import logisticspipes.asm.te.LPTileEntityObject;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.ticks.LPTickHandler;
import logisticspipes.ticks.QueuedTasks;
import network.rs485.logisticspipes.connection.ConnectionType;
import network.rs485.logisticspipes.world.CoordinateUtils;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class TEControl {

    /**
     * Called when a block entity is loaded/placed.
     * <p>
     * Previously injected into ALL TileEntities via ASM. Now called directly from
     * {@link LogisticsTileGenericPipe#onLoad()} for LP pipes.
     * <p>
     * Non-LP neighbour changes are handled by BlockChangeListener which
     * listens for BlockEvent.EntityPlaceEvent / BlockEvent.BreakEvent.
     * Previously injected into ALL TileEntities via ASM.
     */
    public static void validate(final BlockEntity be) {
        final Level level = be.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        if (be.getClass().getName().startsWith("net.minecraft.world.level.block.entity")) {
            return;
        }

        final DoubleCoordinates pos = new DoubleCoordinates(be);
        if (pos.getXInt() == 0 && pos.getYInt() <= 0 && pos.getZInt() == 0) {
            return;
        }

        if (!(be instanceof ILPTEInformation ilpteInformation)) {
            return;
        }
        if (SimpleServiceLocator.pipeInformationManager.isPipe(be, false, ConnectionType.UNDEFINED)
            || SimpleServiceLocator.specialtileconnection.isType(be)) {
            ilpteInformation.setLPTileEntityObject(new LPTileEntityObject());
            Objects.requireNonNull(ilpteInformation.getLPTileEntityObject()).initialised =
                LPTickHandler.getWorldInfo(level).getWorldTick();
            if (ilpteInformation.getLPTileEntityObject().initialised < 5) {
                return;
            }
            QueuedTasks.queueTask(() -> {
                if (!SimpleServiceLocator.pipeInformationManager.isPipe(be, true, ConnectionType.UNDEFINED)) {
                    return null;
                }
                for (Direction dir : Direction.values()) {
                    DoubleCoordinates newPos = CoordinateUtils.sum(pos, dir);
                    if (level.isLoaded(newPos.getBlockPos()) && !newPos.blockExists(level)) {
                        continue;
                    }
                    BlockEntity nextTile = newPos.getTileEntity(level);
                    if (nextTile instanceof ILPTEInformation nextInformation
                        && nextInformation.getLPTileEntityObject() != null) {
                        if (SimpleServiceLocator.pipeInformationManager.isItemPipe(nextTile)) {
                            SimpleServiceLocator.pipeInformationManager.getInformationProviderFor(nextTile)
                                .refreshTileCacheOnSide(dir.getOpposite());
                        }
                        if (SimpleServiceLocator.pipeInformationManager.isItemPipe(be)) {
                            SimpleServiceLocator.pipeInformationManager.getInformationProviderFor(be)
                                .refreshTileCacheOnSide(dir);
                            SimpleServiceLocator.pipeInformationManager.getInformationProviderFor(be)
                                .refreshTileCacheOnSide(dir.getOpposite());
                        }
                        var listeners = new ArrayList<>(nextInformation.getLPTileEntityObject().changeListeners);
                        for (ITileEntityChangeListener listener : listeners) {
                            listener.pipeAdded(pos, dir.getOpposite());
                        }
                    }
                }
                return null;
            });
        }
    }

    /**
     * Called when a block entity is invalidated/removed.
     * <p>
     * Previously injected into ALL TileEntities via ASM. Now called directly from
     * {@link LogisticsTileGenericPipe#setRemoved()} for LP pipes.
     * <p>
     * Non-LP neighbours: covered by LogisticsEventListener.onNeighborNotify (BlockEvent.NeighborNotifyEvent)
     * which flags adjacent routers for recheck when any block changes.
     */
    public static void invalidate(final BlockEntity be) {
        final Level level = be.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        if (be instanceof LogisticsTileGenericPipe logisticsTileGenericPipe
            && logisticsTileGenericPipe.isRoutingPipe()) {
            return;
        }
        if (!(be instanceof ILPTEInformation ilpteInformation)) {
            return;
        }
        if (ilpteInformation.getLPTileEntityObject() != null) {
            QueuedTasks.queueTask(() -> {
                DoubleCoordinates pos = new DoubleCoordinates(be);
                for (Direction dir : Direction.values()) {
                    DoubleCoordinates newPos = CoordinateUtils.sum(pos, dir);
                    if (level.isLoaded(newPos.getBlockPos()) && !newPos.blockExists(level)) {
                        continue;
                    }
                    BlockEntity nextTile = newPos.getTileEntity(level);
                    if (nextTile instanceof ILPTEInformation nextInformation
                        && nextInformation.getLPTileEntityObject() != null) {
                        if (SimpleServiceLocator.pipeInformationManager.isItemPipe(nextTile)) {
                            SimpleServiceLocator.pipeInformationManager.getInformationProviderFor(nextTile)
                                .refreshTileCacheOnSide(dir.getOpposite());
                        }
                    }
                }
                var listeners = new ArrayList<>(ilpteInformation.getLPTileEntityObject().changeListeners);
                for (ITileEntityChangeListener listener : listeners) {
                    listener.pipeRemoved(pos);
                }
                return null;
            });
        }
    }
}
