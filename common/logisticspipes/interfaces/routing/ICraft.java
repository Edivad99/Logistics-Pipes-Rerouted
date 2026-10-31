package logisticspipes.interfaces.routing;

import org.jspecify.annotations.Nullable;

import logisticspipes.request.ICraftingTemplate;
import logisticspipes.request.promise.IPromise;
import logisticspipes.request.resources.IResource;

public interface ICraft extends IProvide {

	void registerExtras(IPromise promise);

	@Nullable
	ICraftingTemplate addCrafting(IResource type);

	boolean canCraft(IResource toCraft);

	int getTodo();
}
