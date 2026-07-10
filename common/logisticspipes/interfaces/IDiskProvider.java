package logisticspipes.interfaces;

import javax.annotation.Nonnull;
import logisticspipes.utils.gui.ItemDisplay;
import net.minecraft.world.item.ItemStack;

public interface IDiskProvider {

	@Nonnull
	ItemStack getDisk();

	int getX();

	int getY();

	int getZ();

	ItemDisplay getItemDisplay();
}
