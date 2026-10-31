package logisticspipes.interfaces.routing;

import java.util.Map;

import logisticspipes.request.promise.FluidPromise;
import logisticspipes.routing.order.IOrderInfoProvider;
import logisticspipes.routing.order.IOrderInfoProvider.ResourceType;
import logisticspipes.utils.FluidIdentifier;

public interface IProvideFluids extends IProvide {

	Map<FluidIdentifier, Integer> getAvailableFluids();

	IOrderInfoProvider fullFill(FluidPromise promise, IRequestFluid destination, ResourceType type, IAdditionalTargetInformation info);

}
