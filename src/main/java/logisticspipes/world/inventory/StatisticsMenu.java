package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

import lombok.Getter;

import logisticspipes.blocks.stats.LogisticsStatisticsTileEntity;

/**
 * The statistics table has no slots of its own -- not even the player's inventory -- but still
 * needs a menu, so the screen has something to be attached to.
 */
public class StatisticsMenu extends DummyMenu {

    @Getter
    private final LogisticsStatisticsTileEntity blockEntity;

    public StatisticsMenu(int containerId, Inventory inventory, LogisticsStatisticsTileEntity blockEntity) {
        super(LPMenuTypes.STATISTICS.get(), containerId, inventory.player, blockEntity);
        this.blockEntity = blockEntity;
    }
}
