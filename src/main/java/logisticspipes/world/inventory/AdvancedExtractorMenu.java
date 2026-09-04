package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

import lombok.Getter;

import logisticspipes.network.ModuleTarget;

import network.rs485.logisticspipes.module.AsyncAdvancedExtractor;

/**
 * The advanced extractor's filter, which is the plain nine-slot filter plus a switch for whether
 * the list includes or excludes.
 */
public class AdvancedExtractorMenu extends ModuleMenu {

    @Getter
    private final AsyncAdvancedExtractor extractor;

    public AdvancedExtractorMenu(int containerId, Inventory inventory, ModuleTarget target,
        AsyncAdvancedExtractor extractor) {
        super(LPMenuTypes.ADVANCED_EXTRACTOR.get(), containerId, inventory, target, extractor);
        this.extractor = extractor;
        addNormalSlotsForPlayerInventory(inventory, 8, 60);
        for (int slot = 0; slot < 9; slot++) {
            addDummySlot(slot, extractor.getFilterInventory(), 8 + slot * 18, 18);
        }
    }
}
