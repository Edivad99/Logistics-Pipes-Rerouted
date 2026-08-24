package logisticspipes.interfaces;

import net.minecraft.world.item.ItemStack;

import logisticspipes.utils.gui.ItemDisplay;

public interface IDiskProvider {

	ItemStack getDisk();

	int getX();

	int getY();

	int getZ();

	ItemDisplay getItemDisplay();
}
