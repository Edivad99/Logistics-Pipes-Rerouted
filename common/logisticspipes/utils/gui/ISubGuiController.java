package logisticspipes.utils.gui;

import javax.annotation.Nullable;

public interface ISubGuiController {

	void setSubGui(SubGuiScreen gui);

	void resetSubGui();

	boolean hasSubGui();

	@Nullable SubGuiScreen getSubGui();

	LogisticsBaseGuiScreen getBaseScreen();

}
