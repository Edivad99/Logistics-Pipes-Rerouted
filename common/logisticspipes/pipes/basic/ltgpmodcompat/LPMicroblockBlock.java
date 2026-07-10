package logisticspipes.pipes.basic.ltgpmodcompat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import logisticspipes.interfaces.ITickable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base class for LP pipe blocks.
 * MCMultiPart integration (IMultipartContainerBlock) was removed for 1.20.1 — no 1.20.1 port exists.
 * Previously extended BlockContainer; now extends Block and implements EntityBlock directly.
 */
public abstract class LPMicroblockBlock extends Block implements EntityBlock {

	public LPMicroblockBlock(Properties properties) {
		super(properties);
	}

	@Nullable
	@Override
	public abstract BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state);

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
		return (lvl, pos, st, be) -> {
			if (be instanceof ITickable) ((ITickable) be).update();
		};
	}
}
