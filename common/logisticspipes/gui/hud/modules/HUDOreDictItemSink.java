package logisticspipes.gui.hud.modules;

import java.util.List;

import logisticspipes.gui.hud.HudChassisPipe;
import logisticspipes.interfaces.IHUDButton;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.modules.ModuleOreDictItemSink;
import logisticspipes.renderer.HUDDrawContext;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;

public class HUDOreDictItemSink implements IHUDModuleRenderer {

	private final ModuleOreDictItemSink itemSink;

	public HUDOreDictItemSink(ModuleOreDictItemSink module) {
		itemSink = module;
	}

	@Override
	public void renderContent(HUDDrawContext context, boolean shifted) {
		ItemStackRenderer.renderItemIdentifierStackListIntoHud(context, itemSink.getHudItemList(), null, 0, HudChassisPipe.MODULE_CONTENT_LEFT, -32, 3, 9, 18, 18, 100.0F, DisplayAmount.NEVER, false, shifted);
	}

	@Override
	public List<IHUDButton> getButtons() {
		return null;
	}
}
