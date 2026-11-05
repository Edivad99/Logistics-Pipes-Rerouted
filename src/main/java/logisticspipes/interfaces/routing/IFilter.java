package logisticspipes.interfaces.routing;

import net.minecraft.core.BlockPos;

import logisticspipes.request.resources.IResource;
import logisticspipes.utils.item.ItemIdentifier;

/**
 * What a firewall on a route lets through.
 */
public interface IFilter {

    /**
     * @return true when the listed items are the ones blocked, false when they are the only ones
     * let through
     */
    boolean isBlocked();

    boolean isFilteredItem(ItemIdentifier item);

    boolean isFilteredItem(IResource resource);

    boolean blockProvider();

    boolean blockCrafting();

    boolean blockRouting();

    boolean blockPower();

    BlockPos getPos();
}
