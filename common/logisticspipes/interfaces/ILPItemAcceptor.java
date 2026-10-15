package logisticspipes.interfaces;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;

public interface ILPItemAcceptor {

	boolean accept(LogisticsGenericPipeBlockEntity pipe, Direction from, ItemStack stack);
}
