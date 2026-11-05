package logisticspipes.interfaces;

import java.util.List;

/**
 * Supplies the routing debug HUDs, which are drawn in place of the normal ones while it is set.
 */
public interface IDebugHUDProvider {

    List<IHeadUpDisplayRendererProvider> getHUDs();
}
