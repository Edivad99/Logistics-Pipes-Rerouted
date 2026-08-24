package logisticspipes.interfaces;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.pipes.basic.CoreRoutedPipe.ItemSendMode;
import logisticspipes.routing.IRouter;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.tuples.Pair;

public interface ISendRoutedItem {

	int getSourceID();

	IRouter getRouter();

	IRoutedItem sendStack(ItemStack stack, Pair<Integer, SinkReply> reply, ItemSendMode mode, Direction direction);

	IRoutedItem sendStack(ItemStack stack, int destination, ItemSendMode mode, IAdditionalTargetInformation info, Direction direction);

	default IRoutedItem sendStack(ItemStack stack, int destRouterId, SinkReply sinkReply, ItemSendMode itemSendMode, Direction direction) {
		return sendStack(stack, new Pair<>(destRouterId, sinkReply), itemSendMode, direction);
	}
}
