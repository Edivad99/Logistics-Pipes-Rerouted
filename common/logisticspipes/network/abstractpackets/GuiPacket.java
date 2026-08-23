package logisticspipes.network.abstractpackets;

import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SubGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public abstract class GuiPacket extends ModernPacket {

	public GuiPacket(int id) {
		super(id);
	}

	protected <T> T getGui(Class<T> guiClass) {
		Screen currentScreen = Minecraft.getInstance().screen;
		if (currentScreen == null) {
			return null;
		}
		if (guiClass.isAssignableFrom(currentScreen.getClass())) {
			return (T) currentScreen;
		}
		SubGuiScreen subScreen = null;
		if (currentScreen instanceof LogisticsBaseGuiScreen) {
			subScreen = ((LogisticsBaseGuiScreen) currentScreen).getSubGui();
		}
		while (subScreen != null) {
			if (guiClass.isAssignableFrom(subScreen.getClass())) {
				return (T) subScreen;
			}
			subScreen = subScreen.getSubGui();
		}
		return null;
	}
}
