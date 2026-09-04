package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

import lombok.Getter;

import logisticspipes.pipes.basic.fluid.FluidSinkPipe;

/**
 * One fluid filter slot, for the basic fluid pipe.
 */
public class FluidSinkMenu extends DummyMenu {

    @Getter
    private final FluidSinkPipe pipe;

    public FluidSinkMenu(int containerId, Inventory inventory, FluidSinkPipe pipe) {
        super(LPMenuTypes.FLUID_SINK.get(), containerId, inventory.player, pipe.container);
        this.pipe = pipe;
        addFluidSlot(0, pipe.getSinkInv(), 28, 13);
        addNormalSlotsForPlayerInventory(inventory, 10, 45);
    }
}
