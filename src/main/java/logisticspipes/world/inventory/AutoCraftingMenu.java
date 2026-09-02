package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

import lombok.Getter;

import logisticspipes.world.level.block.entity.LogisticsCraftingTableBlockEntity;

public class AutoCraftingMenu extends DummyMenu {

    @Getter
    private final LogisticsCraftingTableBlockEntity blockEntity;

    public AutoCraftingMenu(int containerId, Inventory inventory, LogisticsCraftingTableBlockEntity blockEntity) {
        super(LPMenuTypes.AUTO_CRAFTING.get(), containerId, inventory.player, blockEntity);
        this.blockEntity = blockEntity;
        final boolean fuzzy = blockEntity.isFuzzy();
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                final int slot = y * 3 + x;
                if (fuzzy) {
                    addFuzzyDummySlot(slot, blockEntity.matrix, 35 + x * 18, 10 + y * 18,
                        blockEntity.inputFuzzy(slot));
                } else {
                    addDummySlot(slot, blockEntity.matrix, 35 + x * 18, 10 + y * 18);
                }
            }
        }
        if (fuzzy) {
            addFuzzyUnmodifiableSlot(0, blockEntity.resultInv, 125, 28, blockEntity.outputFuzzy());
        } else {
            addUnmodifiableSlot(0, blockEntity.resultInv, 125, 28);
        }
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 9; x++) {
                addNormalSlot(y * 9 + x, blockEntity.inv, 8 + x * 18, 80 + y * 18);
            }
        }
        addNormalSlotsForPlayerInventory(inventory, 9, 136);
    }
}
