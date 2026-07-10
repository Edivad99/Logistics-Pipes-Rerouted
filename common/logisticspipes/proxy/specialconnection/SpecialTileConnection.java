package logisticspipes.proxy.specialconnection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import logisticspipes.interfaces.routing.ISpecialTileConnection;
import logisticspipes.logisticspipes.IRoutedItem;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SpecialTileConnection {

	private List<ISpecialTileConnection> handler = new ArrayList<>();

	public void registerHandler(ISpecialTileConnection connectionHandler) {
		if (connectionHandler.init()) {
			handler.add(connectionHandler);
		}
	}

	public Collection<BlockEntity> getConnectedPipes(BlockEntity tile) {
		for (ISpecialTileConnection connectionHandler : handler) {
			if (connectionHandler.isType(tile)) {
				return connectionHandler.getConnections(tile);
			}
		}
		return new ArrayList<>();
	}

	public boolean needsInformationTransition(BlockEntity tile) {
		for (ISpecialTileConnection connectionHandler : handler) {
			if (connectionHandler.isType(tile)) {
				return connectionHandler.needsInformationTransition();
			}
		}
		return false;
	}

	public void transmit(BlockEntity tile, IRoutedItem arrivingItem) {
		for (ISpecialTileConnection connectionHandler : handler) {
			if (connectionHandler.isType(tile)) {
				connectionHandler.transmit(tile, arrivingItem);
				break;
			}
		}
	}

	public boolean isType(BlockEntity tile) {
		for (ISpecialTileConnection connectionHandler : handler) {
			if (connectionHandler.isType(tile)) {
				return true;
			}
		}
		return false;
	}
}
