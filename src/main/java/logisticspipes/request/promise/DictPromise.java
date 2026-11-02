package logisticspipes.request.promise;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IProvideItems;
import logisticspipes.request.order.IOrderInfoProvider;
import logisticspipes.request.resources.DictResource;

public class DictPromise extends ItemPromise {

    /**
     * The item, its fuzzy flags and its requester. The amount is {@link #numberOfItems}, as for any
     * item promise: it still changes after the promise is made.
     */
    private final DictResource resource;

    public DictPromise(DictResource item, int stackSize, IProvideItems sender,
        IOrderInfoProvider.@Nullable ResourceType type) {
        super(item.getItem(), stackSize, sender, type);
        // A copy: the caller's resource is often a crafting template's result, reused for every promise.
        this.resource = item.copy();
    }

    /**
     * A new resource for what is promised right now.
     *
     * <p>A copy because it goes on to become an order, and the order counts its own amount down.
     */
    public DictResource getResource() {
        DictResource copy = resource.copy();
        copy.stack.setStackSize(numberOfItems);
        return copy;
    }

    @Override
    public IExtraPromise split(int more) {
        numberOfItems -= more;
        return new ExtraDictPromise(resource, more, sender, false);
    }
}
