package logisticspipes.world.level.block;

import java.util.Collections;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

import logisticspipes.pipes.basic.CoreMultiBlockPipe;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;
import logisticspipes.pipes.basic.LogisticsTileGenericSubMultiBlock;
import logisticspipes.world.level.block.entity.LPBlockEntityTypes;

public class LogisticsGenericSubMultiBlockBlock extends Block implements EntityBlock {

    /**
     * Fallback so the helper block stays targetable when no main pipe geometry is found.
     */
    private static final VoxelShape FALLBACK_SHAPE = Shapes.box(0.25, 0.25, 0.25, 0.75, 0.75, 0.75);
    public static boolean redirectedToMainPipe = false;
    public static @Nullable BlockPos CURRENT_CREATED_MULTI_BLOCK;

    public LogisticsGenericSubMultiBlockBlock(Properties properties) {
        super(properties.strength(1.5F).noOcclusion().dynamicShape().forceSolidOn());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Sub-multiblocks are invisible helpers; the main pipe's model draws the visible geometry.
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof LogisticsTileGenericSubMultiBlock multiBlock) {
            VoxelShape shape = Shapes.empty();
            for (LogisticsGenericPipeBlockEntity mainPipe : multiBlock.getConnectedMainPipes()) {
                if (mainPipe.isMultiBlock() && mainPipe.pipe instanceof CoreMultiBlockPipe multiBlockPipe) {
                    shape = Shapes.or(shape, LogisticsGenericPipeBlock.getMultiBlockShape(multiBlockPipe, pos));
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
            for (LogisticsGenericPipeBlockEntity mainPipe : multiBlock.getConnectedMainPipes()) {
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
        return new LogisticsTileGenericSubMultiBlock(pos, state,
            LogisticsGenericSubMultiBlockBlock.CURRENT_CREATED_MULTI_BLOCK);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
        BlockEntityType<T> type) {
        return level.isClientSide() ? null
            : BaseEntityBlock.createTickerHelper(type, LPBlockEntityTypes.SUB_PIPE.get(),
            LogisticsTileGenericSubMultiBlock::serverTick);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        // Sub-blocks drop nothing; the main pipe block entity handles drops.
        return Collections.emptyList();
    }

    /**
     * A creative player gets no drops, so the main pipes are removed here, before the sub block
     * goes and its block entity would destroy them with drops. Doors do the same with their
     * other half.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops()
            && level.getBlockEntity(pos) instanceof LogisticsTileGenericSubMultiBlock multiBlock) {
            for (LogisticsGenericPipeBlockEntity mainPipe : multiBlock.getConnectedMainPipes()) {
                if (mainPipe.isMultiBlock()) {
                    level.removeBlock(mainPipe.getBlockPos(), false);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
        @Nullable Orientation orientation, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, orientation, isMoving);
        BlockEntity tile = level.getBlockEntity(pos);
        if (tile instanceof LogisticsTileGenericSubMultiBlock multiBlock) {
            multiBlock.scheduleNeighborChange();
        }
    }
}
