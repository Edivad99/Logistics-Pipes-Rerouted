package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

import lombok.Getter;

import logisticspipes.logic.LogicController;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;

/**
 * The logic controller layout screen of a pipe.
 */
public class LogicControllerMenu extends DummyMenu {

    @Getter
    private final LogicController controller;

    public LogicControllerMenu(int containerId, Inventory inventory, LogisticsGenericPipeBlockEntity container) {
        super(LPMenuTypes.LOGIC_CONTROLLER.get(), containerId, inventory.player, container);
        this.controller = container.getLogicController();
        addNormalSlotsForPlayerInventory(inventory, 50, 205);
    }
}
