package logisticspipes.items;

// net.minecraft.world.item.CreativeModeTab removed — use CreativeModeTab


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
