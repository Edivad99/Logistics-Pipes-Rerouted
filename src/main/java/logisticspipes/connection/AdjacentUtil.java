package logisticspipes.connection;

import java.util.List;

import org.jspecify.annotations.Nullable;

import logisticspipes.api.connection.Adjacent;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.inventory.IInventoryUtil;

public final class AdjacentUtil {

    private AdjacentUtil() {
    }

    /** The entries are nullable: a neighbour can be an inventory without exposing an inventory util. */
    public static List<@Nullable IInventoryUtil> sneakyInventoryUtils(
        Adjacent adjacent, ISlotUpgradeManager upgradeManager) {
        return adjacent.inventories().stream()
            .map(neighbor -> NeighborBlockEntityUtil.getInventoryUtil(
                NeighborBlockEntityUtil.sneakyInsertion(neighbor).from(upgradeManager)))
            .toList();
    }
}
