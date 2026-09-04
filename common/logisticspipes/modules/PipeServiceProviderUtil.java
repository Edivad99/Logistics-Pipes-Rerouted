package logisticspipes.modules;

import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.ISlotUpgradeManager;

import network.rs485.logisticspipes.connection.LPNeighborTileEntityKt;
import network.rs485.logisticspipes.connection.NeighborTileEntity;

/**
 * The inventories a pipe can reach, seen from the side a sneaky upgrade points at.
 *
 * <p>An entry is null where the neighbour turned out to have no inventory on that side.
 */
public final class PipeServiceProviderUtil {

    private PipeServiceProviderUtil() {
    }

    public static List<@Nullable IInventoryUtil> availableSneakyInventories(IPipeServiceProvider service,
        @Nullable Direction sneakyDirection) {
        if (sneakyDirection == null) {
            return availableInventories(service);
        }
        return neighbours(service).stream()
            .map(adjacent -> LPNeighborTileEntityKt.sneakyInsertion(adjacent).from(sneakyDirection))
            .map(LPNeighborTileEntityKt::getInventoryUtil)
            .toList();
    }

    public static List<@Nullable IInventoryUtil> availableSneakyInventories(IPipeServiceProvider service,
        ISlotUpgradeManager upgradeManager) {
        return neighbours(service).stream()
            .map(adjacent -> LPNeighborTileEntityKt.sneakyInsertion(adjacent).from(upgradeManager))
            .map(LPNeighborTileEntityKt::getInventoryUtil)
            .toList();
    }

    public static List<@Nullable IInventoryUtil> availableInventories(IPipeServiceProvider service) {
        return neighbours(service).stream()
            .map(LPNeighborTileEntityKt::getInventoryUtil)
            .toList();
    }

    private static List<NeighborTileEntity<BlockEntity>> neighbours(IPipeServiceProvider service) {
        return service.getAvailableAdjacent().inventories();
    }
}
