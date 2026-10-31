package logisticspipes.connection.special;

import java.util.Collection;

import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.routing.item.IRoutedItem;

public interface ISpecialBlockEntityConnection {

	boolean isType(BlockEntity blockEntity);

	Collection<BlockEntity> getConnections(BlockEntity blockEntity);

	boolean needsInformationTransition();

	void transmit(BlockEntity blockEntity, IRoutedItem arrivingItem);
}
