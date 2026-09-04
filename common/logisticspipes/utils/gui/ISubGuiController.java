package logisticspipes.utils.gui;

import logisticspipes.client.gui.screen.LogisticsBaseGuiScreen;

import org.jspecify.annotations.Nullable;

public interface ISubGuiController {

	void setSubGui(SubGuiScreen gui);

	void resetSubGui();

	boolean hasSubGui();

	@Nullable SubGuiScreen getSubGui();

	/**
	 * Opens {@code popup} under the deepest sub GUI currently open.
	 *
	 * <p>A popup belongs to the bottom of the stack, not to this screen: opening one while another
	 * is up would otherwise replace it.
	 */
	default void pushSubGui(SubGuiScreen popup) {
		ISubGuiController deepest = this;
		while (deepest.hasSubGui()) {
			final SubGuiScreen sub = deepest.getSubGui();
			if (sub == null) {
				break;
			}
			deepest = sub;
		}
		deepest.setSubGui(popup);
	}

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

	LogisticsBaseGuiScreen<?> getBaseScreen();

}
