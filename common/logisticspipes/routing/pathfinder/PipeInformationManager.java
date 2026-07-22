package logisticspipes.routing.pathfinder;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import logisticspipes.LogisticsPipes;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import network.rs485.logisticspipes.connection.ConnectionType;

public class PipeInformationManager {

	private Map<Class<?> /*BlockEntity*/, Class<? extends IPipeInformationProvider>> infoProvider = new HashMap<>();

    @Nullable
	public IPipeInformationProvider getInformationProviderFor(@Nullable BlockEntity tile) {
        switch (tile) {
            case null -> {
                return null;
            }
            case IPipeInformationProvider iPipeInformationProvider -> {
                return iPipeInformationProvider;
            }
            case ISubMultiBlockPipeInformationProvider iSubMultiBlockPipeInformationProvider -> {
                return iSubMultiBlockPipeInformationProvider.getMainTile();
            }
            default -> {
                for (Class<?> type : infoProvider.keySet()) {
                    if (type.isAssignableFrom(tile.getClass())) {
                        try {
                            IPipeInformationProvider provider = infoProvider.get(type).getDeclaredConstructor(type).newInstance(type.cast(tile));
                            if (provider.isCorrect(ConnectionType.UNDEFINED)) {
                                return provider;
                            }
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | IllegalArgumentException | SecurityException |
                                 NoSuchMethodException e) {
                            LogisticsPipes.LOG.error("Failed to instantiate IPipeInformationProvider for type {}", type.getName(), e);
                        }
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

	public boolean isItemPipe(@Nullable BlockEntity tile) {
		return isPipe(tile, true, ConnectionType.ITEM);
	}

	public boolean isPipe(@Nullable BlockEntity tile) {
		return isPipe(tile, true, ConnectionType.UNDEFINED);
	}

	public boolean isPipe(@Nullable BlockEntity tile, boolean check, ConnectionType pipeType) {
        switch (tile) {
            case null -> {
                return false;
            }
            case IPipeInformationProvider __ -> {
                return true;
            }
            case ISubMultiBlockPipeInformationProvider __ -> {
                return pipeType == ConnectionType.MULTIBLOCK;
            }
            default -> {
                for (Class<?> type : infoProvider.keySet()) {
                    if (type.isAssignableFrom(tile.getClass())) {
                        try {
                            IPipeInformationProvider provider = infoProvider.get(type).getDeclaredConstructor(type).newInstance(type.cast(tile));
                            if (!check || provider.isCorrect(pipeType)) {
                                return true;
                            }
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | IllegalArgumentException |
                                 NoSuchMethodException | SecurityException e) {
                            LogisticsPipes.LOG.error("Failed to check IPipeInformationProvider for type {}", type.getName(), e);
                        }
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
