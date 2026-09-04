package logisticspipes.interfaces;

import logisticspipes.utils.item.ItemIdentifierInventory;

/**
 * A pipe that holds a frequency card.
 *
 * <p>The two ends of an inventory system -- entrance and destination -- share nothing but this
 * one slot and the screen that shows it, and no supertype said so.
 */
public interface IFreqCardHolder {

    ItemIdentifierInventory getFreqCardInventory();
}
