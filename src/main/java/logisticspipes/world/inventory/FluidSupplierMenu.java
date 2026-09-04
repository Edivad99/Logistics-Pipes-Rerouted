package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

import lombok.Getter;

import logisticspipes.pipes.PipeItemsFluidSupplier;

/**
 * The 3x3 filter grid of a fluid supplier pipe.
 */
public class FluidSupplierMenu extends DummyMenu {

    @Getter
    private final PipeItemsFluidSupplier pipe;

    public FluidSupplierMenu(int containerId, Inventory inventory, PipeItemsFluidSupplier pipe) {
        super(LPMenuTypes.FLUID_SUPPLIER.get(), containerId, inventory.player, pipe.container);
        this.pipe = pipe;
        addNormalSlotsForPlayerInventory(inventory, 18, 97);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addDummySlot(column + row * 3, pipe.getDummyInventory(), 72 + column * 18, 18 + row * 18);
            }
        }
    }
}
