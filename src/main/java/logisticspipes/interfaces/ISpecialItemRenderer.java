package logisticspipes.interfaces;

import logisticspipes.utils.item.ItemIdentifier;

/**
 * A screen that draws something extra over an item in its item list.
 */
public interface ISpecialItemRenderer {

    void specialItemRendering(ItemIdentifier item, int x, int y);
}
