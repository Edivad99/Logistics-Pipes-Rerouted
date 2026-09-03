package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

import lombok.Getter;

import logisticspipes.pipes.PipeFluidRequestLogistics;

/**
 * The request screen of the fluid request pipe.
 */
public class FluidOrdererMenu extends OrdererMenu {

    @Getter
    private final PipeFluidRequestLogistics pipe;

    public FluidOrdererMenu(int containerId, Inventory inventory, PipeFluidRequestLogistics pipe) {
        super(LPMenuTypes.FLUID_ORDERER.get(), containerId, inventory, OrdererMenu.targetOf(pipe));
        this.pipe = pipe;
    }
}
