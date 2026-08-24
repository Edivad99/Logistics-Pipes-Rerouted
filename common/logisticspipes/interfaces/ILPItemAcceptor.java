package logisticspipes.interfaces;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

public interface ILPItemAcceptor {

	boolean accept(LogisticsTileGenericPipe pipe, Direction from, ItemStack stack);
}
