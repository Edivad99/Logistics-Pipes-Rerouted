package logisticspipes.transport;

import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.pipes.PipeItemsInvSysConnector;
import logisticspipes.routing.item.ItemRoutingInformation;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * The transport of the Inventory System Connector: tells the connector about every item it puts
 * into its inventory, so the connector can pick it up again on the far side of its channel.
 */
public class TransportInvConnection extends PipeTransportLogistics {

    public TransportInvConnection() {
        super(true);
    }

    /**
     * Lets fluid containers and other items that may not stay in a normal inventory through: the
     * inventory is only a bridge, and the connector at the other end of the channel takes them out
     * again. Any other pipe destroys them instead of inserting them.
     */
    @Override
    protected boolean isItemUnwanted(ItemIdentifierStack stack) {
        return false;
    }

    /**
     * Called once the item is in the inventory, so there is a handler there; the connector checks
     * itself that the inventory is one it is connected to.
     */
    @Override
    protected void inventorySystemConnectorHook(ItemRoutingInformation info, BlockEntity blockEntity) {
        if (getPipe() instanceof PipeItemsInvSysConnector connector) {
            connector.handleItemEnterInv(info, blockEntity);
        }
    }
}
