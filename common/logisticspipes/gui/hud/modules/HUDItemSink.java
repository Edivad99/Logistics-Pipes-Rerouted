package logisticspipes.gui.hud.modules;

import java.util.List;

import logisticspipes.gui.hud.HudChassisPipe;
import logisticspipes.interfaces.IHUDButton;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.modules.ModuleItemSink;
import logisticspipes.renderer.HUDDrawContext;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;

public class HUDItemSink implements IHUDModuleRenderer {

	private final ModuleItemSink module;

	public HUDItemSink(ModuleItemSink module) {
		this.module = module;
	}

	@Override
	public void renderContent(HUDDrawContext context, boolean shifted) {
		ItemStackRenderer.renderItemIdentifierStackListIntoHud(context, ItemIdentifierStack.getListFromInventory(module.getFilterInventory()), null, 0, HudChassisPipe.MODULE_CONTENT_LEFT, -32, 3, 9, 18, 18, 100.0F, DisplayAmount.NEVER, false, shifted);
	}

	@Override
	public List<IHUDButton> getButtons() {
		return null;
	}
}
