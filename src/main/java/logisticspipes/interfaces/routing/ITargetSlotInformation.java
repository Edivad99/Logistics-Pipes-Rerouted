package logisticspipes.interfaces.routing;

/**
 * Target information for an item meant for one slot of an inventory.
 */
public interface ITargetSlotInformation {

    int getTargetSlot();

    /**
     * @return how many the slot should end up holding
     */
    int getAmount();

    /**
     * @return whether what does not fit the slot goes back, instead of into the rest of the inventory
     */
    boolean isLimited();
}
