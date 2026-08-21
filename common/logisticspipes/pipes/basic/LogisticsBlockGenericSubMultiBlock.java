package logisticspipes.pipes.basic;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import logisticspipes.interfaces.ITickable;
import logisticspipes.world.level.block.LPBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class LogisticsBlockGenericSubMultiBlock extends Block implements EntityBlock {

	public static boolean redirectedToMainPipe = false;

	public LogisticsBlockGenericSubMultiBlock(Properties properties) {
		super(properties.strength(1.5F).noOcclusion());
	}

	@Override
    public RenderShape getRenderShape(BlockState state) {
		// Sub-multiblocks are invisible helpers; main pipe BER renders the visible geometry.
		return RenderShape.INVISIBLE;
	}

	/** Fallback so the helper block stays targetable when no main pipe geometry is found. */
	private static final VoxelShape FALLBACK_SHAPE = Shapes.box(0.25, 0.25, 0.25, 0.75, 0.75, 0.75);

	@Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		BlockEntity tile = world.getBlockEntity(pos);
		if (tile instanceof LogisticsTileGenericSubMultiBlock) {
			VoxelShape shape = Shapes.empty();
			for (LogisticsTileGenericPipe mainPipe : ((LogisticsTileGenericSubMultiBlock) tile).getConnectedMainPipes()) {
				if (mainPipe.isMultiBlock() && mainPipe.pipe instanceof CoreMultiBlockPipe) {
					shape = Shapes.or(shape, LogisticsBlockGenericPipe.getMultiBlockShape((CoreMultiBlockPipe) mainPipe.pipe, pos));
				}
			}
			if (!shape.isEmpty()) {
				return shape;
			}
		}
		return FALLBACK_SHAPE;
	}

	@Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return getShape(state, world, pos, context);
	}

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData,
        Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof LogisticsTileGenericSubMultiBlock multiBlock) {
            for (LogisticsTileGenericPipe mainPipe : multiBlock.getConnectedMainPipes()) {
                if (mainPipe.isMultiBlock()) {
                    BlockState mainState = level.getBlockState(mainPipe.getBlockPos());
                    ItemStack pick = LPBlocks.PIPE.get()
                        .getCloneItemStack(level, mainPipe.getBlockPos(), mainState, includeData, player);
                    if (!pick.isEmpty()) {
                        return pick;
                    }
                }
            }
        }
        return super.getCloneItemStack(level, pos, state, includeData, player);
    }

    @Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		if (LogisticsBlockGenericSubMultiBlock.currentCreatedMultiBlock == null && logisticspipes.proxy.MainProxy.isServer(null)) {
			new RuntimeException("Unknown MultiBlock controller").printStackTrace();
		}
		return new LogisticsTileGenericSubMultiBlock(pos, state, LogisticsBlockGenericSubMultiBlock.currentCreatedMultiBlock);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return (lvl, pos, st, be) -> {
			if (be instanceof ITickable) ((ITickable) be).update();
		};
	}

	@Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		// Sub-blocks drop nothing; the main pipe block entity handles drops.
		return Collections.emptyList();
	}

	/*
	@Override
	public TextureAtlasSprite getIcon(int p_149691_1_, int p_149691_2_) {
		return LogisticsPipes.LogisticsPipeBlock.getIcon(p_149691_1_, p_149691_2_);
	}

	// TODO: getIcon(IBlockAccess, int, int, int, int) removed in 1.20.1 — rendering rewrite needed (deferred)
	// @Override
	@OnlyIn(Dist.CLIENT)
	@SuppressWarnings({ "all" })
	// getIcon_DEAD — stub removed; IBlockAccess and TextureAtlasSprite-based getIcon removed in 1.20.1
	*/

	public static DoubleCoordinates currentCreatedMultiBlock;

	@Override
	public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() != newState.getBlock()) {
			if (redirectedToMainPipe) {
				super.onRemove(state, worldIn, pos, newState, isMoving);
				return;
			}
			BlockEntity tile = worldIn.getBlockEntity(pos);
			if (tile instanceof LogisticsTileGenericSubMultiBlock) {
				List<LogisticsTileGenericPipe> mainPipeList = ((LogisticsTileGenericSubMultiBlock) tile).getMainPipe();
				mainPipeList.stream()
						.filter(Objects::nonNull)
						.filter(LogisticsTileGenericPipe::isMultiBlock)
						.forEach(mainPipe -> {
							redirectedToMainPipe = true;
							BlockState mainState = worldIn.getBlockState(mainPipe.getBlockPos());
							LPBlocks.PIPE.get().onRemove(mainState, worldIn, mainPipe.getBlockPos(), mainState, false);
							redirectedToMainPipe = false;
							worldIn.removeBlock(mainPipe.getBlockPos(), false);
						});
			}
		}
		super.onRemove(state, worldIn, pos, newState, isMoving);
	}

	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
        @Nullable Orientation orientation, boolean isMoving) {
		super.neighborChanged(state, level, pos, block, orientation, isMoving);
		BlockEntity tile = level.getBlockEntity(pos);
		if (tile instanceof LogisticsTileGenericSubMultiBlock) {
			((LogisticsTileGenericSubMultiBlock) tile).scheduleNeighborChange();
		}
	}

}
