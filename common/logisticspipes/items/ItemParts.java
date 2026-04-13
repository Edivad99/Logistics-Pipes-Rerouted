package logisticspipes.items;

import javax.annotation.Nonnull;

// net.minecraft.world.item.CreativeModeTab removed — use CreativeModeTab
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;

public class ItemParts extends LogisticsItem {

	public ItemParts() {
		super();
	}

	@Override
	public int getModelCount() {
		return 4;
	}

	@Override
	public String getModelSubdir() {
		return "parts";
	}

	// fillItemCategory removed in 1.20.1 — creative tab content registered via BuildCreativeModeTabContentsEvent

}
