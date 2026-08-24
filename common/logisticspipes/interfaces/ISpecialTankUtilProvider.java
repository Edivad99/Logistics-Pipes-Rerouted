package logisticspipes.interfaces;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jspecify.annotations.Nullable;

/**
 * Supplies an {@link ITankUtil} for a block that holds fluids in a way {@code IFluidHandler} cannot
 * express -- a storage network, where there are no tanks to enumerate.
 *
 * <p>This is the fluid counterpart of {@code SpecialInventoryHandler.Factory}, and it is consulted
 * before the {@code Capabilities.FluidHandler.BLOCK} lookup in {@code PipeFluidUtil}. As on the item
 * side, claiming a block means owning it: the capability fallback no longer runs for it, so a
 * provider must serve every operation, not just the reads it cares about.</p>
 */
public interface ISpecialTankUtilProvider {

	boolean init();

	boolean isType(BlockEntity blockEntity, @Nullable Direction dir);

	@Nullable
	ITankUtil getTankUtilFor(BlockEntity blockEntity, @Nullable Direction dir);
}
