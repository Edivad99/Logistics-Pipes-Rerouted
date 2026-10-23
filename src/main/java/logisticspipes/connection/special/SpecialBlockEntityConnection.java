package logisticspipes.connection.special;

import java.util.Collection;
import java.util.List;

import net.minecraft.world.level.block.entity.BlockEntity;

import org.jspecify.annotations.Nullable;

import logisticspipes.logisticspipes.IRoutedItem;

public final class SpecialBlockEntityConnection {

	private static @Nullable SpecialBlockEntityConnection instance;

	private final List<ISpecialBlockEntityConnection> handler;

	private SpecialBlockEntityConnection(List<ISpecialBlockEntityConnection> handler) {
		this.handler = handler;
	}

	/** Collects everything registered on {@code event}, which is closed from here on. */
	public static void load(RegisterSpecialConnectionsEvent event) {
		instance = new SpecialBlockEntityConnection(event.registeredBlockEntityConnections());
	}

	/**
	 * The block entity connection handlers collected at startup.
	 *
	 * @throws IllegalStateException before startup collected them
	 */
	public static SpecialBlockEntityConnection get() {
		SpecialBlockEntityConnection current = instance;
		if (current == null) {
			throw new IllegalStateException("SpecialBlockEntityConnection is not loaded yet");
		}
		return current;
	}

	public Collection<BlockEntity> getConnectedPipes(BlockEntity blockEntity) {
		for (ISpecialBlockEntityConnection connectionHandler : handler) {
			if (connectionHandler.isType(blockEntity)) {
				return connectionHandler.getConnections(blockEntity);
			}
		}
		return List.of();
	}

	public boolean needsInformationTransition(@Nullable BlockEntity blockEntity) {
        if (blockEntity == null) {
            return false;
        }
		for (ISpecialBlockEntityConnection connectionHandler : handler) {
			if (connectionHandler.isType(blockEntity)) {
				return connectionHandler.needsInformationTransition();
			}
		}
		return false;
	}

	public void transmit(BlockEntity blockEntity, IRoutedItem arrivingItem) {
		for (ISpecialBlockEntityConnection connectionHandler : handler) {
			if (connectionHandler.isType(blockEntity)) {
				connectionHandler.transmit(blockEntity, arrivingItem);
				break;
			}
		}
	}

	public boolean isType(BlockEntity blockEntity) {
		for (ISpecialBlockEntityConnection connectionHandler : handler) {
			if (connectionHandler.isType(blockEntity)) {
				return true;
			}
		}
		return false;
	}
}
