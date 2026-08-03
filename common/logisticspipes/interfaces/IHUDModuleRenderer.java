package logisticspipes.interfaces;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;

public interface IHUDModuleRenderer {

	void renderContent(GuiGraphics guiGraphics, boolean shifted);

	List<IHUDButton> getButtons();
}
