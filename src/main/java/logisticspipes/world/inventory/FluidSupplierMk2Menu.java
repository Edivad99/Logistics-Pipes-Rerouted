package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

import lombok.Getter;

import logisticspipes.pipes.PipeFluidSupplierMk2;

/**
 * The single fluid slot of a mk2 fluid supplier.
 */
public class FluidSupplierMk2Menu extends DummyMenu {

    @Getter
    private final PipeFluidSupplierMk2 pipe;

    public FluidSupplierMk2Menu(int containerId, Inventory inventory, PipeFluidSupplierMk2 pipe) {
        super(LPMenuTypes.FLUID_SUPPLIER_MK2.get(), containerId, inventory.player, pipe.container);
        this.pipe = pipe;
        addNormalSlotsForPlayerInventory(inventory, 13, 92);
        addFluidSlot(0, pipe.getDummyInventory(), 60, 18);
    }
}
