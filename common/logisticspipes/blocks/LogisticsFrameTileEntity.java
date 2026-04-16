package logisticspipes.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import logisticspipes.LPRegistries;

/**
 * Minimal BlockEntity for the Logistics Block Frame.
 * Has no logic — exists solely so the frame can use the ENTITYBLOCK_ANIMATED
 * render path and be drawn by LogisticsSolidBlockRenderer.
 */
public class LogisticsFrameTileEntity extends LogisticsSolidTileEntity {

	public LogisticsFrameTileEntity(BlockPos pos, BlockState state) {
		super(LPRegistries.BE_FRAME.get(), pos, state);
	}
}
