package logisticspipes.request.order;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.inventory.IExternalCrafter;
import logisticspipes.request.resources.DictResource;

/**
 * A crafting order a provider handed on to an autocrafting system behind its inventory, such as an
 * Applied Energistics 2 network.
 */
public class ExternalCraftingOrder extends ItemOrder {

    /**
     * The slot of the provider module the order belongs to, telling it apart from those of the other
     * modules sharing the pipe's orders.
     */
    @Getter
    private final int ownerSlot;
    /**
     * The request handed to the system; null after a reload, until the provider takes the order
     * back.
     */
    @Getter
    @Setter
    private IExternalCrafter.@Nullable Job job;
    /**
     * Whether the system had taken the request on before the world was saved.
     */
    private final boolean acceptedBeforeSave;

    public ExternalCraftingOrder(DictResource item, IRequestItems destination,
        @Nullable IAdditionalTargetInformation info, int ownerSlot, IExternalCrafter.@Nullable Job job,
        boolean acceptedBeforeSave) {
        super(item, destination, ResourceType.CRAFTING, info);
        this.ownerSlot = ownerSlot;
        this.job = job;
        this.acceptedBeforeSave = acceptedBeforeSave;
    }

    /**
     * Whether the system has taken the request on, and is now making it.
     */
    public boolean isAccepted() {
        return job == null ? acceptedBeforeSave : job.state() == IExternalCrafter.State.ACCEPTED;
    }

    /**
     * Whether the system turned the request down.
     */
    public boolean hasFailed() {
        return job != null && job.state() == IExternalCrafter.State.FAILED;
    }
}
