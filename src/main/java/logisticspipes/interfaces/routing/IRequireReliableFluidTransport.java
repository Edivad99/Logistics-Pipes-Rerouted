package logisticspipes.interfaces.routing;

import logisticspipes.utils.FluidIdentifier;

/**
 * A pipe that keeps count of the fluid on its way to it, and so has to hear how that ends.
 */
public interface IRequireReliableFluidTransport {

    void liquidLost(FluidIdentifier fluid, int amount);

    void liquidNotInserted(FluidIdentifier fluid, int amount);

    void liquidArrived(FluidIdentifier fluid, int amount);
}
