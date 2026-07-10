package logisticspipes.interfaces;

import javax.annotation.Nonnull;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public interface ILPItemAcceptor {

	boolean accept(LogisticsTileGenericPipe pipe, Direction from, @Nonnull ItemStack stack);
}
