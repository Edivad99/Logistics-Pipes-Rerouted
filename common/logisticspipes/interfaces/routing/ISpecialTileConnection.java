package logisticspipes.interfaces.routing;

import java.util.Collection;
import logisticspipes.logisticspipes.IRoutedItem;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface ISpecialTileConnection {

	boolean init();

	boolean isType(BlockEntity tile);

	Collection<BlockEntity> getConnections(BlockEntity tile);

	boolean needsInformationTransition();

	void transmit(BlockEntity tile, IRoutedItem arrivingItem);
}
