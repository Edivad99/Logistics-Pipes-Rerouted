package logisticspipes.world.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import lombok.Getter;

import logisticspipes.pipes.PipeItemsInvSysConnector;

/**
 * The inventory system connector's screen, which shows the pipe's contents and the channel it is
 * on. The pipe pushes both to whoever has it open, so it is told who is watching.
 */
public class InvSysConMenu extends DummyMenu {

    @Getter
    private final PipeItemsInvSysConnector pipe;

    public InvSysConMenu(int containerId, Inventory inventory, PipeItemsInvSysConnector pipe) {
        super(LPMenuTypes.INV_SYS_CON.get(), containerId, inventory.player, null);
        this.pipe = pipe;
        addNormalSlotsForPlayerInventory(inventory, 11, 136);
        if (inventory.player instanceof ServerPlayer) {
            pipe.screenOpenedByPlayer(inventory.player);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer) {
            pipe.screenClosedByPlayer(player);
        }
    }
}
