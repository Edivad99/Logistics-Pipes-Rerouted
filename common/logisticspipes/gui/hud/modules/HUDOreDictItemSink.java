package logisticspipes.gui.hud.modules;

import java.util.List;

import net.minecraft.client.Minecraft;





import logisticspipes.interfaces.IHUDButton;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.modules.ModuleOreDictItemSink;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;

public class HUDOreDictItemSink implements IHUDModuleRenderer {

	private final ModuleOreDictItemSink itemSink;

	public HUDOreDictItemSink(ModuleOreDictItemSink module) {
		itemSink = module;
	}

	@Override
	public void renderContent(boolean shifted) {
		Minecraft mc = Minecraft.getInstance();
		ItemStackRenderer.renderItemIdentifierStackListIntoGui(itemSink.getHudItemList(), null, 0, -25, -32, 3, 9, 18, 18, 100.0F, DisplayAmount.NEVER, false, shifted);
	}

	@Override
	public List<IHUDButton> getButtons() {
		return null;
	}
}
