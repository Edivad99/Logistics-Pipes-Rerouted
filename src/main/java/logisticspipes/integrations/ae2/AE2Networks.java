package logisticspipes.integrations.ae2;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;

import appeng.api.ids.AEBlockIds;
import appeng.api.ids.AEPartIds;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.api.storage.MEStorage;
import org.jspecify.annotations.Nullable;

/**
 * Finds the Applied Energistics network behind a block, shared by the item and fluid handlers and
 * the autocrafting bridge.
 *
 * <p>Every class here touches {@code appeng} types, so it must only be loaded once AE2 is known to
 * be present -- see {@code SpecialInventoryHandlerManager}.</p>
 */
public final class AE2Networks {

    private AE2Networks() {
    }

    /**
     * The storage of the network behind the ME Interface or null when there is none to talk to.
     */
    @Nullable
    public static MEStorage findStorage(BlockEntity blockEntity, @Nullable Direction dir) {
        IGrid grid = AE2Networks.findActiveGrid(blockEntity, dir);
        if (grid == null) {
            return null;
        }
        IStorageService storageService = grid.getStorageService();
        return storageService.getInventory();
    }

    /**
     * The network behind the ME Interface or null when there is none to talk to.
     */
    @Nullable
    public static IGrid findActiveGrid(BlockEntity blockEntity, @Nullable Direction dir) {
        if (!(blockEntity instanceof IInWorldGridNodeHost host)) {
            return null;
        }
        IGridNode node = AE2Networks.findInterfaceNode(blockEntity, host, dir);
        // An unbooted or unpowered network reports nothing rather than pretending to be empty-ish:
        // isActive() covers both, and asking a down network for its inventory is meaningless.
        if (node == null || !node.isActive()) {
            return null;
        }
        return node.getGrid();
    }

    @Nullable
    private static IGridNode findInterfaceNode(BlockEntity blockEntity, IInWorldGridNodeHost host,
        @Nullable Direction dir) {
        if (AEBlockIds.INTERFACE.equals(BuiltInRegistries.BLOCK.getKey(blockEntity.getBlockState().getBlock()))) {
            return AE2Networks.findNode(host, dir);
        }
        if (!(host instanceof IPartHost partHost)) {
            return null;
        }
        if (dir != null) {
            return AE2Networks.interfacePartNode(partHost.getPart(dir));
        }
        // No side given: any interface part on the cable bus leads to the same grid.
        for (Direction side : Direction.values()) {
            IGridNode node = AE2Networks.interfacePartNode(partHost.getPart(side));
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    @Nullable
    private static IGridNode interfacePartNode(@Nullable IPart part) {
        if (part == null || !AEPartIds.INTERFACE.equals(IPartItem.getId(part.getPartItem()))) {
            return null;
        }
        return part.getGridNode();
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
