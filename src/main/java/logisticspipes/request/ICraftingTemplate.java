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

    /**
     * How many of {@code wanted} crafting sets the crafter can actually make, for a crafter that
     * brings its own ingredients and so can run short of them outside the network.
     */
    default int getMaxCraftingSets(int wanted) {
        return wanted;
    }

    /**
     * What the crafter itself lacks to make {@code sets} crafting sets, shown as missing when a
     * request fails. Only for crafters that bring their own ingredients.
     */
    default List<IResource> getMissingFor(int sets) {
        return List.of();
    }

    /**
     * One thing a crafting set needs, and where in the crafter it has to go.
     */
    record Requirement(IResource resource, @Nullable IAdditionalTargetInformation info) {}
}
