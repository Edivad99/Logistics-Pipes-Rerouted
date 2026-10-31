package logisticspipes.request;

import java.util.List;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.ICraft;
import logisticspipes.interfaces.routing.ICraftItems;
import logisticspipes.request.promise.IExtraPromise;
import logisticspipes.request.promise.IPromise;
import logisticspipes.request.resources.IResource;
import logisticspipes.utils.item.ItemIdentifierStack;

public interface ICraftingTemplate extends Comparable<ICraftingTemplate> {

	List<Requirement> getComponents(int nCraftingSets);

	List<IExtraPromise> getByproducts(int workSets);

	int getResultStackSize();

	IPromise generatePromise(int nCraftingSetsNeeded);

	ICraft getCrafter();

	int getPriority();

	boolean canCraft(IResource requestType);

	IResource getResultItem();

	int comparePriority(int priority);

	int compareStack(ItemIdentifierStack stack);

	int compareCrafter(ICraftItems crafter);

	/** One thing a crafting set needs, and where in the crafter it has to go. */
	record Requirement(IResource resource, @Nullable IAdditionalTargetInformation info) {}
}
