package logisticspipes.gui.hud.modules;

import java.util.List;

import logisticspipes.interfaces.IHUDButton;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.utils.gui.SimpleGraphics;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;
import network.rs485.logisticspipes.module.SimpleFilter;

public class HUDSimpleFilterModule implements IHUDModuleRenderer {

    private final SimpleFilter filter;

    public HUDSimpleFilterModule(SimpleFilter filter) {
        this.filter = filter;
    }

    @Override
    public void renderContent(boolean shifted) {
        ItemStackRenderer.renderItemIdentifierStackListIntoGui(SimpleGraphics.guiGraphics,
            ItemIdentifierStack.getListFromInventory(filter.getFilterInventory()), null,
            0, -25, -32, 3, 9, 18, 18, 100.0F, DisplayAmount.NEVER, false, shifted);
    }

    @Override
    public List<IHUDButton> getButtons() {
        return null;
    }
}
