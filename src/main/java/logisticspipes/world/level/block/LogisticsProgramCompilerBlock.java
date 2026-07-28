package logisticspipes.world.level.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import logisticspipes.blocks.LogisticsSolidBlock;
import logisticspipes.interfaces.IGuiTileEntity;
import logisticspipes.interfaces.ITickable;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.world.level.block.entity.LPBlockEntityTypes;
import logisticspipes.world.level.block.entity.LogisticsProgramCompilerBlockEntity;

public class LogisticsProgramCompilerBlock extends LogisticsSolidBlock implements EntityBlock {

    public LogisticsProgramCompilerBlock(Properties properties) {
        super(Type.LOGISTICS_PROGRAM_COMPILER, properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
        BlockHitResult hitResult) {
        if (!player.isCrouching()) {
            if (player instanceof ServerPlayer serverPlayer) {
                level.getBlockEntity(pos, LPBlockEntityTypes.PROGRAM_COMPILER.get())
                    .ifPresent(blockEntity -> {
                        serverPlayer.openMenu(blockEntity, pos);
                    });
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogisticsProgramCompilerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
        BlockEntityType<T> type) {
        // Tick all ITickable solid block entities
        return (lvl, pos, st, be) -> {
            if (be instanceof ITickable tickable) {
                tickable.update();
            }
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
}
