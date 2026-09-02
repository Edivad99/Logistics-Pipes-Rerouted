package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

import lombok.Getter;

import logisticspipes.world.level.block.entity.LogisticsPowerJunctionBlockEntity;

public class PowerJunctionMenu extends DummyMenu {

    @Getter
    private final LogisticsPowerJunctionBlockEntity blockEntity;

    public PowerJunctionMenu(int containerId, Inventory inventory, LogisticsPowerJunctionBlockEntity blockEntity) {
        super(LPMenuTypes.POWER_JUNCTION.get(), containerId, inventory.player, blockEntity);
        this.blockEntity = blockEntity;
        addNormalSlotsForPlayerInventory(inventory, 8, 80);
    }
}
