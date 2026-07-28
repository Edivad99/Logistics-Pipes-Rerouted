package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

import lombok.Getter;

import logisticspipes.world.item.LPItems;
import logisticspipes.world.level.block.entity.LogisticsProgramCompilerBlockEntity;

public class ProgramCompilerMenu extends DummyMenu {

    @Getter
    private final LogisticsProgramCompilerBlockEntity blockEntity;

    public ProgramCompilerMenu(int containerId, Inventory inventory, LogisticsProgramCompilerBlockEntity blockEntity) {
        super(LPMenuTypes.PROGRAM_COMPILER.get(), containerId, inventory.player, blockEntity);
        this.blockEntity = blockEntity;
        addRestrictedSlot(0, blockEntity.getInventory(), 10, 10, LPItems.DISK.get());
        addRestrictedSlot(1, blockEntity.getInventory(), 154, 10, LPItems.LOGISTICS_PROGRAMMER.get());
        addNormalSlotsForPlayerInventory(inventory, 11, 106);
    }
}
