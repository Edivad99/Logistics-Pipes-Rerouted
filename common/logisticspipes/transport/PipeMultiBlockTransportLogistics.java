package logisticspipes.transport;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jspecify.annotations.Nullable;

import logisticspipes.pipes.basic.CoreMultiBlockPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemClient;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemServer;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;
import logisticspipes.world.level.block.entity.LogisticsGenericSubMultiBlockBlockEntity;

public class PipeMultiBlockTransportLogistics extends PipeTransportLogistics {

	private @Nullable CoreMultiBlockPipe multiPipe;

	public PipeMultiBlockTransportLogistics() {
		super(false);
	}

	@Override
	public boolean canPipeConnect(BlockEntity tile, Direction side) {
		if (tile instanceof LogisticsGenericPipeBlockEntity
                && ((LogisticsGenericPipeBlockEntity) tile).pipe != null && ((LogisticsGenericPipeBlockEntity) tile).pipe.isHSTube()) {
			return true;
		}
		if (tile instanceof LogisticsGenericSubMultiBlockBlockEntity
            && ((LogisticsGenericSubMultiBlockBlockEntity) tile).getMainPipe() != null && !((LogisticsGenericSubMultiBlockBlockEntity) tile).getMainPipe().isEmpty()) {
			for (LogisticsGenericPipeBlockEntity pipe : ((LogisticsGenericSubMultiBlockBlockEntity) tile).getMainPipe()) {
				if (pipe.pipe == null || !pipe.pipe.isHSTube()) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public @Nullable CoreMultiBlockPipe getMultiPipe() {
		if (multiPipe == null) {
			if (getPipe() instanceof CoreMultiBlockPipe coreMultiBlockPipe) {
				multiPipe = coreMultiBlockPipe;
			}
		}
		return multiPipe;
	}

	@Override
	public float getPipeLength() {
		if (getMultiPipe() != null) {
			return getMultiPipe().getPipeLength();
		}
		return super.getPipeLength();
	}

	public double getDistanceWeight() {
		if (getMultiPipe() != null) {
			return getMultiPipe().getDistanceWeight();
		}
		return super.getDistanceWeight();
	}

	@Override
	public float getYawDiff(LPTravelingItem item) {
		if (getMultiPipe() != null) {
			return getMultiPipe().getYawDiff(item);
		}
		return super.getYawDiff(item);
	}

	@Override
	public RoutingResult resolveDestination(LPTravelingItemServer data) {
		if (getMultiPipe() == null) {
			return new RoutingResult(null, false);
		}
		return new RoutingResult(getMultiPipe().getExitForInput(data.input.getOpposite()), true);
	}

	@Override
	protected void reachedEnd(LPTravelingItem item) {
		BlockEntity tile = null;
		if (getMultiPipe() != null) {
			tile = getMultiPipe().getConnectedEndTile(item.output);
		}
		if (items.scheduleRemoval(item)) {
			if (!container.getLevel().isClientSide()) {
				handleTileReachedServer((LPTravelingItemServer) item, tile, item.output);
			} else {
				handleTileReachedClient((LPTravelingItemClient) item, tile, item.output);
			}
		}
	}

	@Override
	protected void handleTileReachedServer(LPTravelingItemServer arrivingItem, @Nullable BlockEntity tile, @Nullable Direction dir) {
		markChunkModified(tile);
		if (tile instanceof LogisticsGenericPipeBlockEntity && ((LogisticsGenericPipeBlockEntity) tile).pipe instanceof CoreMultiBlockPipe) {
			passToNextPipe(arrivingItem, tile);
			return;
		} else if (tile instanceof LogisticsGenericSubMultiBlockBlockEntity) {
			List<LogisticsGenericPipeBlockEntity> masterTile = ((LogisticsGenericSubMultiBlockBlockEntity) tile).getMainPipe();
			if (!masterTile.isEmpty()) {
				if (masterTile.size() > 1) {
					throw new UnsupportedOperationException();
				}
				passToNextPipe(arrivingItem, masterTile.get(0));
				return;
			}
		}
		spawnMisroutedItemExplosionEffect();
	}

	@Override
	protected void handleTileReachedClient(LPTravelingItemClient arrivingItem, @Nullable BlockEntity tile, @Nullable Direction dir) {
		if (tile instanceof LogisticsGenericPipeBlockEntity && ((LogisticsGenericPipeBlockEntity) tile).pipe instanceof CoreMultiBlockPipe) {
			passToNextPipe(arrivingItem, tile);
			return;
		} else if (tile instanceof LogisticsGenericSubMultiBlockBlockEntity) {
			List<LogisticsGenericPipeBlockEntity> masterTile = ((LogisticsGenericSubMultiBlockBlockEntity) tile).getMainPipe();
			if (!masterTile.isEmpty()) {
				if (masterTile.size() > 1) {
					throw new UnsupportedOperationException();
				}
				passToNextPipe(arrivingItem, masterTile.get(0));
				return;
			}
		}
		spawnMisroutedItemExplosionEffect();
	}

	@Override
	public void readjustSpeed(LPTravelingItemServer item) {
		item.setSpeed(0.8F);
	}

	@Override
	@Nullable
	public CoreUnroutedPipe getNextPipe(Direction output) {
		BlockEntity tile = null;
		if (getMultiPipe() != null) {
			tile = getMultiPipe().getConnectedEndTile(output);
		}
		if (tile instanceof LogisticsGenericSubMultiBlockBlockEntity) {
			List<LogisticsGenericPipeBlockEntity> list = ((LogisticsGenericSubMultiBlockBlockEntity) tile).getMainPipe();
			if (!list.isEmpty()) {
				if (list.size() > 1) {
					throw new UnsupportedOperationException();
				}
				tile = list.get(0);
			}
		}
		if (tile instanceof LogisticsGenericPipeBlockEntity) {
			return ((LogisticsGenericPipeBlockEntity) tile).pipe;
		}
		return null;
	}

	/**
	 * Plays the sound and particle of an explosion at the pipe, marking an item that reached a tile
	 * it should never have reached.
	 *
	 * <p>This used to build an {@code Explosion} and call {@code finalizeExplosion(true)} on it
	 * without ever calling {@code explode()}, so the block list stayed empty and the only thing that
	 * ever happened was the sound and the particle -- no block or entity damage. 1.21.3 turned
	 * Explosion into an interface implemented by the server-side ServerExplosion, which cannot be
	 * driven that way, so the two effects are produced directly. The values match what the old
	 * constructor defaulted to for a radius-4 DESTROY explosion.</p>
	 */
	private void spawnMisroutedItemExplosionEffect() {
		Level level = this.getWorld();
		final BlockPos pipePos = this.getPipe().getPos();
		double x = pipePos.getX();
		double y = pipePos.getY();
		double z = pipePos.getZ();
		if (level.isClientSide()) {
			level.playLocalSound(x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F,
				(1.0F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2F) * 0.7F, false);
		}
		level.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1.0, 0.0, 0.0);
	}
}
