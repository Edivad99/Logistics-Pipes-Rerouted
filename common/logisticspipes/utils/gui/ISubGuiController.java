package logisticspipes.utils.gui;

import org.jspecify.annotations.Nullable;

public interface ISubGuiController {

	void setSubGui(SubGuiScreen gui);

	void resetSubGui();

	boolean hasSubGui();

	@Nullable SubGuiScreen getSubGui();

	/**
	 * The nearest sub GUI of the given type in the stack below this one, or null when there is
	 * none open.
	 */
	default <T extends SubGuiScreen> @Nullable T findSubGui(Class<T> type) {
		SubGuiScreen sub = getSubGui();
		while (sub != null) {
			if (type.isInstance(sub)) {
				return type.cast(sub);
			}
			sub = sub.getSubGui();
		}
		return null;
	}

	LogisticsBaseGuiScreen getBaseScreen();

}
