package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

import lombok.Getter;

import logisticspipes.logic.LogicController;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * The logic controller layout screen of a pipe.
 */
public class LogicControllerMenu extends DummyMenu {

    @Getter
    private final LogicController controller;

    public LogicControllerMenu(int containerId, Inventory inventory, LogisticsTileGenericPipe container) {
        super(LPMenuTypes.LOGIC_CONTROLLER.get(), containerId, inventory.player, container);
        this.controller = container.getLogicController();
        addNormalSlotsForPlayerInventory(inventory, 50, 205);
    }
}
