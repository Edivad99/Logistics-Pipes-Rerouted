package logisticspipes.interfaces;

import javax.annotation.Nonnull;
import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.pipes.basic.CoreRoutedPipe.ItemSendMode;
import logisticspipes.routing.IRouter;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.tuples.Pair;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public interface ISendRoutedItem {

	int getSourceID();

	@Nonnull
	IRouter getRouter();

	IRoutedItem sendStack(@Nonnull ItemStack stack, Pair<Integer, SinkReply> reply, ItemSendMode mode, @Nonnull Direction direction);

	IRoutedItem sendStack(@Nonnull ItemStack stack, int destination, ItemSendMode mode, IAdditionalTargetInformation info, @Nonnull Direction direction);

	default IRoutedItem sendStack(@Nonnull ItemStack stack, int destRouterId, @Nonnull SinkReply sinkReply, @Nonnull ItemSendMode itemSendMode, @Nonnull Direction direction) {
		return sendStack(stack, new Pair<>(destRouterId, sinkReply), itemSendMode, direction);
	}
}
