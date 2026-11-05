package logisticspipes.interfaces;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * A pipe that takes back what an inventory refused, before the transport sends it on again.
 */
public interface IBufferItems {

    /**
     * @param stack the items to add to the buffer; the buffer keeps a copy or other representation
     * @param info  what the item carried about its target, if anything
     * @return how many items were NOT added to the buffer
     */
    int addToBuffer(ItemIdentifierStack stack, @Nullable IAdditionalTargetInformation info);
}
