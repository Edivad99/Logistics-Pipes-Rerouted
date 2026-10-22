package logisticspipes.inventory.tank;

import java.util.Map;

import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.fluids.FluidStack;

import logisticspipes.api.provider.ISpecialTankHandler;
import logisticspipes.utils.FluidIdentifier;

public interface ISpecialTankAccessHandler extends ISpecialTankHandler {

    Map<FluidIdentifier, Long> getAvailableLiquid(BlockEntity blockEntity);

    FluidStack drainFrom(BlockEntity blockEntity, FluidIdentifier ident, Integer amount, boolean drain);
}
