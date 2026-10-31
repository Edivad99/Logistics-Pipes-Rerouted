package logisticspipes.interfaces;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.pipes.basic.CoreRoutedPipe.ItemSendMode;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.item.IRoutedItem;
import logisticspipes.utils.SinkReply;

public interface ISendRoutedItem {

	int getSourceID();

	IRouter getRouter();

	IRoutedItem sendStack(ItemStack stack, int destination, ItemSendMode mode, @Nullable IAdditionalTargetInformation info, Direction direction);

	IRoutedItem sendStack(ItemStack stack, int destination, SinkReply reply, ItemSendMode mode, Direction direction);
}
