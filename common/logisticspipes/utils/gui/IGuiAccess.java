package logisticspipes.utils.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public interface IGuiAccess {

	int getGuiLeft();

	int getGuiTop();

	int getXSize();

	int getYSize();

	int getRight();

	int getBottom();

	Minecraft getMC();

	GuiGraphics getGuiGraphics();
}
