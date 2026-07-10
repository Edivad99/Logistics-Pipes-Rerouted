package logisticspipes.interfaces;

import java.util.Map;
import logisticspipes.utils.FluidIdentifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;

public interface ISpecialTankAccessHandler extends ISpecialTankHandler {

	Map<FluidIdentifier, Long> getAvailableLiquid(BlockEntity tile);

	FluidStack drainFrom(BlockEntity tile, FluidIdentifier ident, Integer amount, boolean drain);
}
