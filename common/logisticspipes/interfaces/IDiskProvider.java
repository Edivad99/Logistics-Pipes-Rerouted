package logisticspipes.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import logisticspipes.utils.gui.ItemDisplay;

public interface IDiskProvider {

	ItemStack getDisk();

	BlockPos getBlockPos();

	ItemDisplay getItemDisplay();
}
