package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

import lombok.Getter;

import logisticspipes.blocks.powertile.LogisticsPowerProviderTileEntity;

public class PowerProviderMenu extends DummyMenu {

    @Getter
    private final LogisticsPowerProviderTileEntity blockEntity;

    public PowerProviderMenu(int containerId, Inventory inventory, LogisticsPowerProviderTileEntity blockEntity) {
        super(LPMenuTypes.POWER_PROVIDER.get(), containerId, inventory.player, blockEntity);
        this.blockEntity = blockEntity;
        addNormalSlotsForPlayerInventory(inventory, 8, 80);
    }
}
