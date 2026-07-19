package logisticspipes.blocks;

import logisticspipes.world.level.block.entity.LPBlockEntityTypes;
import logisticspipes.world.level.block.entity.LogisticsSolidBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Minimal BlockEntity for the Logistics Block Frame.
 * Has no logic — exists solely so the frame can use the ENTITYBLOCK_ANIMATED
 * render path and be drawn by LogisticsSolidBlockRenderer.
 */
public class LogisticsFrameTileEntity extends LogisticsSolidBlockEntity {

	public LogisticsFrameTileEntity(BlockPos pos, BlockState state) {
		super(LPBlockEntityTypes.FRAME.get(), pos, state);
	}
}
