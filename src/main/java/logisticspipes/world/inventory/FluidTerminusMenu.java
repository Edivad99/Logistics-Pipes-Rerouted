package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

import lombok.Getter;

import logisticspipes.pipes.PipeFluidTerminus;

/**
 * The nine fluid filter slots of a fluid terminus.
 */
public class FluidTerminusMenu extends DummyMenu {

    @Getter
    private final PipeFluidTerminus pipe;

    public FluidTerminusMenu(int containerId, Inventory inventory, PipeFluidTerminus pipe) {
        super(LPMenuTypes.FLUID_TERMINUS.get(), containerId, inventory.player, pipe.container);
        this.pipe = pipe;
        for (int slot = 0; slot < pipe.getSinkInv().getContainerSize(); slot++) {
            addFluidSlot(slot, pipe.getSinkInv(), 10 + slot * 18, 19);
        }
        addNormalSlotsForPlayerInventory(inventory, 10, 45);
    }
}
