package logisticspipes.interfaces.routing;

import org.jspecify.annotations.Nullable;

import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.FluidSinkReply;

public interface IFluidSink {

	@Nullable
	FluidSinkReply sinkAmount(FluidIdentifierStack stack);
}
