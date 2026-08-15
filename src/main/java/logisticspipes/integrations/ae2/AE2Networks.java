package logisticspipes.integrations.ae2;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.storage.MEStorage;

/**
 * Finds the Applied Energistics network behind a block, shared by the item and fluid handlers.
 *
 * <p>Every class here touches {@code appeng} types, so it must only be loaded once AE2 is known to
 * be present -- see {@code SpecialInventoryHandlerManager}.</p>
 */
public final class AE2Networks {

    private AE2Networks() {
    }

    /**
     * The storage of the network {@code blockEntity} belongs to, or null when there is none to talk to.
     */
    @Nullable
    public static MEStorage findStorage(BlockEntity blockEntity, @Nullable Direction dir) {
        if (!(blockEntity instanceof IInWorldGridNodeHost host)) {
            return null;
        }
        IGridNode node = AE2Networks.findNode(host, dir);
        // An unbooted or unpowered network reports nothing rather than pretending to be empty-ish:
        // isActive() covers both, and asking a down network for its inventory is meaningless.
        if (node == null || !node.isActive()) {
            return null;
        }
        IGrid grid = node.getGrid();
        IStorageService storageService = grid.getStorageService();
        return storageService.getInventory();
    }

    @Nullable
    private static IGridNode findNode(IInWorldGridNodeHost host, @Nullable Direction dir) {
        if (dir != null) {
            return host.getGridNode(dir);
        }
        // No side given: any node this block exposes leads to the same grid.
        for (Direction side : Direction.values()) {
            IGridNode node = host.getGridNode(side);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    /**
     * Identifies us as the machine we are talking through where we can: AE2 uses the action source
     * to attribute the operation, and storage buses use it to avoid feeding a network back into
     * itself.
     */
    public static IActionSource actionSource(BlockEntity blockEntity) {
        return blockEntity instanceof IActionHost host ? IActionSource.ofMachine(host) : IActionSource.empty();
    }
}
