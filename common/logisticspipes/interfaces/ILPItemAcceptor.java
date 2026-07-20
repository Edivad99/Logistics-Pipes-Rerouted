package logisticspipes.interfaces;

import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public interface ILPItemAcceptor {

	boolean accept(LogisticsTileGenericPipe pipe, Direction from, ItemStack stack);
}
