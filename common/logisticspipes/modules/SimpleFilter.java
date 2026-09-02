package logisticspipes.modules;

import net.minecraft.world.Container;

/**
 * A module that filters by plain item, through a nine slot filter.
 *
 * <p>What the modules sharing that screen have in common, and all of it: the inventory the filter
 * lives in.
 */
public interface SimpleFilter {

    Container getFilterInventory();
}
