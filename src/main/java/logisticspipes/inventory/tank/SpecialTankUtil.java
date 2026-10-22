package logisticspipes.inventory.tank;

import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public class SpecialTankUtil extends TankUtil implements ISpecialTankUtil {

    private final BlockEntity blockEntity;
    private final ISpecialTankAccessHandler handler;

    public SpecialTankUtil(ResourceHandler<FluidResource> fluid, BlockEntity blockEntity,
        ISpecialTankAccessHandler handler) {
        super(fluid);
        this.blockEntity = blockEntity;
        this.handler = handler;
    }

    @Override
    public BlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ISpecialTankAccessHandler getSpecialHandler() {
        return handler;
    }
}
