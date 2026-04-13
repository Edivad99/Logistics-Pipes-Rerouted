package logisticspipes.interfaces;

import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;

import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

public interface ILPItemAcceptor {

	boolean accept(LogisticsTileGenericPipe pipe, Direction from, @Nonnull ItemStack stack);
}
