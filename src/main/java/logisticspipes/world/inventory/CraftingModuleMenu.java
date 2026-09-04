package logisticspipes.world.inventory;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Inventory;

import lombok.Getter;

import logisticspipes.modules.ModuleCrafter;
import logisticspipes.network.ModuleTarget;

/**
 * The crafting module's recipe grid, with whatever its upgrades have added around it.
 */
public class CraftingModuleMenu extends ModuleMenu {

    /**
     * What the upgrades on the module make of its screen: how many fluid slots, whether the grid
     * matches loosely, how tall the cleanup area is. The screen is laid out differently for each.
     */
    public record Layout(
            boolean advancedSatellite,
            int fluidSlots,
            boolean byproductExtractor,
            boolean fuzzy,
            int cleanupSize,
            boolean cleanupExcludes,
            int[] fluidAmounts
    ) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Layout> STREAM_CODEC =
                StreamCodec.of((buffer, layout) -> {
                    buffer.writeBoolean(layout.advancedSatellite);
                    buffer.writeVarInt(layout.fluidSlots);
                    buffer.writeBoolean(layout.byproductExtractor);
                    buffer.writeBoolean(layout.fuzzy);
                    buffer.writeVarInt(layout.cleanupSize);
                    buffer.writeBoolean(layout.cleanupExcludes);
                    buffer.writeVarIntArray(layout.fluidAmounts);
                }, buffer -> new Layout(
                        buffer.readBoolean(),
                        buffer.readVarInt(),
                        buffer.readBoolean(),
                        buffer.readBoolean(),
                        buffer.readVarInt(),
                        buffer.readBoolean(),
                        buffer.readVarIntArray()));
    }

    @Getter
    private final ModuleCrafter crafter;

    @Getter
    private final Layout layout;

    public CraftingModuleMenu(int containerId, Inventory inventory, ModuleTarget target, ModuleCrafter crafter,
        Layout layout) {
        super(LPMenuTypes.CRAFTING_MODULE.get(), containerId, inventory, target, crafter);
        this.crafter = crafter;
        this.layout = layout;
        final int panelHeight = layout.advancedSatellite() ? 217 : 187;
        addNormalSlotsForPlayerInventory(inventory, 9, panelHeight - 81);

        for (int slot = 0; slot < 9; slot++) {
            if (layout.fuzzy()) {
                addFuzzyDummySlot(slot, crafter.dummyInventory, 8 + slot * 18, 18, crafter.inputFuzzy(slot));
            } else {
                addDummySlot(slot, crafter.dummyInventory, 8 + slot * 18, 18);
            }
        }

        final int outputY = layout.advancedSatellite() ? 105 : 55;
        if (layout.fuzzy()) {
            addFuzzyDummySlot(9, crafter.dummyInventory, 85, outputY, crafter.outputFuzzy());
        } else {
            addDummySlot(9, crafter.dummyInventory, 85, outputY);
        }

        for (int slot = 0; slot < layout.fluidSlots(); slot++) {
            final int left = layout.advancedSatellite() ? -40 : -(layout.fluidSlots() * 40) + (slot * 40);
            addFluidSlot(slot, crafter.liquidInventory, left + 11, 24);
        }

        if (layout.byproductExtractor()) {
            addDummySlot(10, crafter.dummyInventory, -26, 29);
        }

        for (int row = 0; row < layout.cleanupSize(); row++) {
            for (int column = 0; column < 3; column++) {
                addDummySlot(row * 3 + column, crafter.cleanupInventory, column * 18 - 57, row * 18 + 13);
            }
        }
    }
}
