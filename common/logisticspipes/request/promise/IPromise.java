package logisticspipes.request.promise;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IProvide;
import logisticspipes.request.order.IOrderInfoProvider;
import logisticspipes.request.order.IOrderInfoProvider.ResourceType;
import logisticspipes.request.resources.IResource;
import logisticspipes.utils.item.ItemIdentifier;

public interface IPromise {

	boolean matches(IResource requestType);

	int getAmount();

	IExtraPromise split(int more);

	IProvide getProvider();

	ItemIdentifier getItemType();

	@Nullable
	ResourceType getType();

	@Nullable
	IOrderInfoProvider fullFill(IResource requestType, @Nullable IAdditionalTargetInformation info);

	IPromise copy();
}
