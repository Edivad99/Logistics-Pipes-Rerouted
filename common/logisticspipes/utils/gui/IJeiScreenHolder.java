package logisticspipes.utils.gui;

import org.jspecify.annotations.Nullable;

/**
 * A menu that remembers the screen showing it, for the recipe viewer.
 *
 * <p>JEI hands its transfer handler the menu, but filling a recipe grid needs the screen: only it
 * knows which of the two grid GUIs this is and which block entity it belongs to.
 */
public interface IJeiScreenHolder {

    @Nullable LogisticsBaseGuiScreen getScreenForJEI();

    void setScreenForJEI(LogisticsBaseGuiScreen screen);
}
