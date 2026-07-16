package logisticspipes.routing.pathfinder;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import logisticspipes.LogisticsPipes;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import network.rs485.logisticspipes.connection.ConnectionType;

public class PipeInformationManager {

	private Map<Class<?> /*BlockEntity*/, Class<? extends IPipeInformationProvider>> infoProvider = new HashMap<>();

	public IPipeInformationProvider getInformationProviderFor(BlockEntity tile) {
		if (tile == null) {
			return null;
		}
		if (tile instanceof IPipeInformationProvider) {
			return (IPipeInformationProvider) tile;
		} else if (tile instanceof ISubMultiBlockPipeInformationProvider) {
			return ((ISubMultiBlockPipeInformationProvider) tile).getMainTile();
		} else {
			for (Class<?> type : infoProvider.keySet()) {
				if (type.isAssignableFrom(tile.getClass())) {
					try {
						IPipeInformationProvider provider = infoProvider.get(type).getDeclaredConstructor(type).newInstance(type.cast(tile));
						if (provider.isCorrect(ConnectionType.UNDEFINED)) {
							return provider;
						}
					} catch (InstantiationException | IllegalAccessException | InvocationTargetException | IllegalArgumentException | SecurityException | NoSuchMethodException e) {
						LogisticsPipes.LOG.error("Failed to instantiate IPipeInformationProvider for type {}", type.getName(), e);
					}
				}
			}
		}
		return null;
	}

	public void registerProvider(Class<?> source, Class<? extends IPipeInformationProvider> provider) {
		try {
			provider.getDeclaredConstructor(source);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
		infoProvider.put(source, provider);
	}

	public boolean canConnect(IPipeInformationProvider startPipe, IPipeInformationProvider provider, Direction direction, boolean flag) {
		return startPipe.canConnect(provider.getTile(), direction, flag) && provider.canConnect(startPipe.getTile(), direction.getOpposite(), flag);
	}

	public boolean isItemPipe(BlockEntity tile) {
		return isPipe(tile, true, ConnectionType.ITEM);
	}

	public boolean isPipe(BlockEntity tile) {
		return isPipe(tile, true, ConnectionType.UNDEFINED);
	}

	public boolean isPipe(BlockEntity tile, boolean check, ConnectionType pipeType) {
		if (tile == null) {
			return false;
		}
		if (tile instanceof IPipeInformationProvider) {
			return true;
		} else if (tile instanceof ISubMultiBlockPipeInformationProvider) {
			return pipeType == ConnectionType.MULTIBLOCK;
		} else {
			for (Class<?> type : infoProvider.keySet()) {
				if (type.isAssignableFrom(tile.getClass())) {
					try {
						IPipeInformationProvider provider = infoProvider.get(type).getDeclaredConstructor(type).newInstance(type.cast(tile));
						if (!check || provider.isCorrect(pipeType)) {
							return true;
						}
					} catch (InstantiationException | IllegalAccessException | InvocationTargetException | IllegalArgumentException | NoSuchMethodException | SecurityException e) {
						LogisticsPipes.LOG.error("Failed to check IPipeInformationProvider for type {}", type.getName(), e);
					}
				}
			}
		}
		return false;
	}

	public boolean isNotAPipe(BlockEntity tile) {
		if (tile instanceof IPipeInformationProvider) {
			return false;
		} else if (tile instanceof ISubMultiBlockPipeInformationProvider) {
			return false;
		} else {
			for (Class<?> type : infoProvider.keySet()) {
				if (type.isAssignableFrom(tile.getClass())) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean isFluidPipe(BlockEntity tile) {
		IPipeInformationProvider info = getInformationProviderFor(tile);
		if (info == null) {
			return false;
		}
		return info.isFluidPipe();
	}
}
