package logisticspipes.interfaces.routing;

import org.jspecify.annotations.Nullable;

import logisticspipes.request.ICraftingTemplate;
import logisticspipes.request.promise.IPromise;
import logisticspipes.request.resources.IResource;

/**
 * Something on the network that crafts on request.
 */
public interface ICraft extends IProvide {

    /**
     * Notes the surplus of a craft that was asked for less than a recipe makes, to hand out later.
     */
    void registerExtras(IPromise promise);

    /**
     * @return how to craft {@code toCraft}, or null when this crafter can't
     */
    @Nullable ICraftingTemplate addCrafting(IResource toCraft);

    boolean canCraft(IResource toCraft);

    /**
     * @return how much it still has to make for its orders
     */
    int getTodo();
}
