package logisticspipes.interfaces.routing;

import logisticspipes.utils.FluidIdentifier;

/**
 * A requester of fluids.
 */
public interface IRequestFluid extends IRequest {

    /**
     * An order for this pipe could not be sent.
     */
    void sendFailed(FluidIdentifier fluid, int amount);
}
