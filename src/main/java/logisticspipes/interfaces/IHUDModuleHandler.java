package logisticspipes.interfaces;

import org.jspecify.annotations.Nullable;

/**
 * A module that shows on the HUD of its chassis.
 */
public interface IHUDModuleHandler {

    void startHUDWatching();

    void stopHUDWatching();

    /**
     * @return what draws the module, or null when it has nothing to show
     */
    @Nullable IHUDModuleRenderer getHUDRenderer();
}
