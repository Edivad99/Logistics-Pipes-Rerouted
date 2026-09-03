package logisticspipes.world.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import lombok.Getter;

import logisticspipes.pipes.PipeBlockRequestTable;

/**
 * The request table: a three by three recipe grid, the result it makes, the slot it sorts into,
 * a disk slot, and the nine slots of the table's own inventory.
 */
public class RequestTableMenu extends DummyMenu {

    @Getter
    private final PipeBlockRequestTable table;

    public RequestTableMenu(int containerId, Inventory inventory, PipeBlockRequestTable table) {
        super(LPMenuTypes.REQUEST_TABLE.get(), containerId, inventory.player, null);
        this.table = table;
        // The table pushes the state of the requests it is watching to whoever has the monitor
        // open. It is the pipe that keeps that list, not its block entity, so it is told directly
        // -- the base class only knows how to notify a block entity.
        if (inventory.player instanceof ServerPlayer) {
            table.screenOpenedByPlayer(inventory.player);
        }

        int slot = 0;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addNormalSlot(slot++, table.inv, column * 18 + 20, row * 18 + 80);
            }
        }
        slot = 0;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addDummySlot(slot++, table.matrix, column * 18 + 20, row * 18 + 15);
            }
        }
        addCallableSlotHandler(0, table.resultInv, 101, 33, table::getResultForClick);
        addNormalSlot(0, table.toSortInv, 164, 51);
        addNormalSlot(0, table.diskInv, 164, 25);
        addNormalSlotsForPlayerInventory(inventory, 21, 151);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer) {
            table.screenClosedByPlayer(player);
        }
    }
}
