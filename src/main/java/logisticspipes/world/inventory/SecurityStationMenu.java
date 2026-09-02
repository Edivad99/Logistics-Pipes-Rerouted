package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

import lombok.Getter;

import logisticspipes.blocks.LogisticsSecurityTileEntity;

public class SecurityStationMenu extends DummyMenu {

    @Getter
    private final LogisticsSecurityTileEntity blockEntity;

    public SecurityStationMenu(int containerId, Inventory inventory, LogisticsSecurityTileEntity blockEntity) {
        super(LPMenuTypes.SECURITY_STATION.get(), containerId, inventory.player, blockEntity);
        this.blockEntity = blockEntity;
        addRestrictedSlot(0, blockEntity.inv, 82, 141, null);
        addNormalSlotsForPlayerInventory(inventory, 11, 176);
    }
}
