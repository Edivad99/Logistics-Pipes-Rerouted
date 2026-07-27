package logisticspipes.pipes.basic.ltgpmodcompat;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Intermediate BlockEntity base for LP pipe tile entities.
 * MCMultiPart integration (IMultipartContainer) was removed for 1.20.1 — no 1.20.1 port exists.
 * Previously implemented mcmultipart.api.container.IMultipartContainer.
 */
public abstract class LPMicroblockTileEntity extends BlockEntity {

	public LPMicroblockTileEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/**
     * Returns the level this block entity is in. Replaces removed getWorld() from 1.12.2.
     */
	public @Nullable Level getWorld() {
		return this.level;
	}

	public abstract boolean isMultipartAllowedInPipe();
}
