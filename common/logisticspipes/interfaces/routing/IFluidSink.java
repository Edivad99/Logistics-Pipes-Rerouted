package logisticspipes.interfaces.routing;

import org.jspecify.annotations.Nullable;

import logisticspipes.routing.sink.FluidSinkReply;
import logisticspipes.utils.FluidIdentifierStack;

public interface IFluidSink {

	@Nullable
	FluidSinkReply sinkAmount(FluidIdentifierStack stack);
}
