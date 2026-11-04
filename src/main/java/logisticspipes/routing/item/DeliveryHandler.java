package logisticspipes.routing.item;

import net.minecraft.core.Direction;

import org.jspecify.annotations.Nullable;

/**
 * This class is responsible for handling items arriving at its destination
 *
 * @author Krapht
 */
public abstract class DeliveryHandler {

    public abstract boolean stillWantItem(IRoutedItem item);

    public abstract @Nullable Direction itemArrived(IRoutedItem item, @Nullable Direction denied);

    public void handleItem(IRoutedItem item) {
    }

}
