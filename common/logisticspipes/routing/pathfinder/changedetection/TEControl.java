package logisticspipes.routing.pathfinder.changedetection;

import java.util.ArrayList;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.asm.te.ILPTEInformation;
import logisticspipes.asm.te.ITileEntityChangeListener;
import logisticspipes.asm.te.LPTileEntityObject;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.ticks.LPTickHandler;
import logisticspipes.ticks.QueuedTasks;
import network.rs485.logisticspipes.connection.ConnectionType;
import network.rs485.logisticspipes.world.CoordinateUtils;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class TEControl {

	/**
	 * Called when a block entity is loaded/placed.
	 *
	 * Previously injected into ALL TileEntities via ASM. Now called directly from
	 * {@link LogisticsTileGenericPipe#onLoad()} for LP pipes.
	 *
	 * Non-LP neighbour changes are handled by BlockChangeListener which
	 * listens for BlockEvent.EntityPlaceEvent / BlockEvent.BreakEvent.
	 * Previously injected into ALL TileEntities via ASM.
	 */
	public static void validate(final BlockEntity tile) {
		final Level world = tile.getLevel();
		if (!MainProxy.isServer(world)) {
			return;
		}
		if (tile.getClass().getName().startsWith("net.minecraft.world.level.block.entity")) {
			return;
		}

		final DoubleCoordinates pos = new DoubleCoordinates(tile);
		if (pos.getXInt() == 0 && pos.getYInt() <= 0 && pos.getZInt() == 0) {
			return;
		}

		if (!(tile instanceof ILPTEInformation)) {
			return;
		}
		if (SimpleServiceLocator.pipeInformationManager.isPipe(tile, false, ConnectionType.UNDEFINED) || SimpleServiceLocator.specialtileconnection.isType(tile)) {
			((ILPTEInformation) tile).setLPTileEntityObject(new LPTileEntityObject());
			((ILPTEInformation) tile).getLPTileEntityObject().initialised = LPTickHandler.getWorldInfo(world).getWorldTick();
			if (((ILPTEInformation) tile).getLPTileEntityObject().initialised < 5) {
				return;
			}
			QueuedTasks.queueTask(() -> {
				if (!SimpleServiceLocator.pipeInformationManager.isPipe(tile, true, ConnectionType.UNDEFINED)) {
					return null;
				}
				for (Direction dir : Direction.values()) {
					DoubleCoordinates newPos = CoordinateUtils.sum(pos, dir);
					if (!newPos.blockExists(world)) {
						continue;
					}
					BlockEntity nextTile = newPos.getTileEntity(world);
					if (nextTile instanceof ILPTEInformation && ((ILPTEInformation) nextTile).getLPTileEntityObject() != null) {
						if (SimpleServiceLocator.pipeInformationManager.isItemPipe(nextTile)) {
							SimpleServiceLocator.pipeInformationManager.getInformationProviderFor(nextTile).refreshTileCacheOnSide(dir.getOpposite());
						}
						if (SimpleServiceLocator.pipeInformationManager.isItemPipe(tile)) {
							SimpleServiceLocator.pipeInformationManager.getInformationProviderFor(tile).refreshTileCacheOnSide(dir);
							SimpleServiceLocator.pipeInformationManager.getInformationProviderFor(tile).refreshTileCacheOnSide(dir.getOpposite());
						}
						for (ITileEntityChangeListener listener : new ArrayList<>(((ILPTEInformation) nextTile).getLPTileEntityObject().changeListeners)) {
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
	 *
	 * Previously injected into ALL TileEntities via ASM. Now called directly from
	 * {@link LogisticsTileGenericPipe#setRemoved()} for LP pipes.
	 *
	 * Non-LP neighbours: covered by LogisticsEventListener.onNeighborNotify (BlockEvent.NeighborNotifyEvent)
	 * which flags adjacent routers for recheck when any block changes.
	 */
	public static void invalidate(final BlockEntity tile) {
		final Level world = tile.getLevel();
		if (!MainProxy.isServer(world)) {
			return;
		}
		if (tile instanceof LogisticsTileGenericPipe && ((LogisticsTileGenericPipe) tile).isRoutingPipe()) {
			return;
		}
		if (!(tile instanceof ILPTEInformation)) {
			return;
		}
		if (((ILPTEInformation) tile).getLPTileEntityObject() != null) {
			QueuedTasks.queueTask(() -> {
				DoubleCoordinates pos = new DoubleCoordinates(tile);
				for (Direction dir : Direction.values()) {
					DoubleCoordinates newPos = CoordinateUtils.sum(pos, dir);
					if (!newPos.blockExists(world)) {
						continue;
					}
					BlockEntity nextTile = newPos.getTileEntity(world);
					if (nextTile instanceof ILPTEInformation && ((ILPTEInformation) nextTile).getLPTileEntityObject() != null) {
						if (SimpleServiceLocator.pipeInformationManager.isItemPipe(nextTile)) {
							SimpleServiceLocator.pipeInformationManager.getInformationProviderFor(nextTile).refreshTileCacheOnSide(dir.getOpposite());
						}
					}
				}
				for (ITileEntityChangeListener listener : new ArrayList<>(((ILPTEInformation) tile).getLPTileEntityObject().changeListeners)) {
					listener.pipeRemoved(pos);
				}
				return null;
			});
		}
	}
}
