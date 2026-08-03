package logisticspipes.gui.hud.modules;

import java.util.List;
import logisticspipes.interfaces.IHUDButton;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.modules.ModuleItemSink;
import logisticspipes.utils.gui.SimpleGraphics;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;
import net.minecraft.client.Minecraft;

public class HUDItemSink implements IHUDModuleRenderer {

	private final ModuleItemSink module;

	public HUDItemSink(ModuleItemSink module) {
		this.module = module;
	}

	@Override
	public void renderContent(boolean shifted) {
		ItemStackRenderer.renderItemIdentifierStackListIntoGui(SimpleGraphics.guiGraphics, ItemIdentifierStack.getListFromInventory(module.getFilterInventory()), null, 0, -25, -32, 3, 9, 18, 18, 100.0F, DisplayAmount.NEVER, false, shifted);
	}

	@Override
	public List<IHUDButton> getButtons() {
		return null;
	}
}
