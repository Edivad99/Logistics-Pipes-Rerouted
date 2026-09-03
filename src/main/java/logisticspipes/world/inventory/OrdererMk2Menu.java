package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

import lombok.Getter;

import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;

/**
 * The request screen of the MK2 pipe, which also shows the disk it holds.
 */
public class OrdererMk2Menu extends OrdererMenu {

    @Getter
    private final PipeItemsRequestLogisticsMk2 pipe;

    public OrdererMk2Menu(int containerId, Inventory inventory, PipeItemsRequestLogisticsMk2 pipe) {
        super(LPMenuTypes.ORDERER_MK2.get(), containerId, inventory, OrdererMenu.targetOf(pipe));
        this.pipe = pipe;
    }
}
