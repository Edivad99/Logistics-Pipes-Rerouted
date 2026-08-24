package logisticspipes.utils.gui;

import org.jspecify.annotations.Nullable;

public interface ISubGuiController {

	void setSubGui(SubGuiScreen gui);

	void resetSubGui();

	boolean hasSubGui();

	@Nullable SubGuiScreen getSubGui();

	LogisticsBaseGuiScreen getBaseScreen();

}
