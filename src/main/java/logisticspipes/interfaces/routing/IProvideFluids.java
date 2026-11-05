package logisticspipes.interfaces.routing;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import logisticspipes.request.order.IOrderInfoProvider;
import logisticspipes.request.order.IOrderInfoProvider.ResourceType;
import logisticspipes.request.promise.FluidPromise;
import logisticspipes.utils.FluidIdentifier;

/**
 * A provider of fluids.
 */
public interface IProvideFluids extends IProvide {

    Map<FluidIdentifier, Integer> getAvailableFluids();

    IOrderInfoProvider fullFill(FluidPromise promise, IRequestFluid destination, ResourceType type,
        @Nullable IAdditionalTargetInformation info);
}
