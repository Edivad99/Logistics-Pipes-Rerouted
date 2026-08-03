package logisticspipes.gui.hud.modules;

import java.util.List;
import logisticspipes.gui.hud.HudChassisPipe;
import logisticspipes.interfaces.IHUDButton;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.modules.ModuleOreDictItemSink;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;
import net.minecraft.client.gui.GuiGraphics;

public class HUDOreDictItemSink implements IHUDModuleRenderer {

	private final ModuleOreDictItemSink itemSink;

	public HUDOreDictItemSink(ModuleOreDictItemSink module) {
		itemSink = module;
	}

	@Override
	public void renderContent(GuiGraphics guiGraphics, boolean shifted) {
		ItemStackRenderer.renderItemIdentifierStackListIntoGui(guiGraphics, itemSink.getHudItemList(), null, 0, HudChassisPipe.MODULE_CONTENT_LEFT, -32, 3, 9, 18, 18, 100.0F, DisplayAmount.NEVER, false, shifted);
	}

	@Override
	public List<IHUDButton> getButtons() {
		return null;
	}
}
