package logisticspipes.interfaces.routing;

import org.jspecify.annotations.Nullable;

import logisticspipes.routing.sink.FluidSinkReply;
import logisticspipes.utils.FluidIdentifierStack;

/**
 * A pipe that takes fluids from the network, unasked.
 */
public interface IFluidSink {

    /**
     * @return how much it takes, or null when it takes none
     */
    @Nullable FluidSinkReply sinkAmount(FluidIdentifierStack stack);
}
