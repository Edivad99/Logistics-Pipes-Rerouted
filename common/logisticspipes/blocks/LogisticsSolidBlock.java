package logisticspipes.blocks;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import lombok.Getter;

import logisticspipes.blocks.crafting.LogisticsCraftingTableTileEntity;
import logisticspipes.blocks.powertile.LogisticsPowerJunctionTileEntity;
import logisticspipes.blocks.powertile.LogisticsRFPowerProviderTileEntity;
import logisticspipes.blocks.stats.LogisticsStatisticsTileEntity;
import logisticspipes.interfaces.IGuiTileEntity;
import logisticspipes.interfaces.IRotationProvider;
import logisticspipes.interfaces.ITickable;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.world.level.block.entity.LogisticsProgramCompilerBlockEntity;
import logisticspipes.world.level.block.entity.LogisticsSolidBlockEntity;

public class LogisticsSolidBlock extends Block implements EntityBlock {

    public static final IntegerProperty rotationProperty = IntegerProperty.create("rotation", 0, 3);
    public static final BooleanProperty active = BooleanProperty.create("active");
    public static final Map<Direction, BooleanProperty> connectionPropertys = Arrays.stream(Direction.values())
        .collect(Collectors.toMap(key -> key, key -> BooleanProperty.create("connection_" + key.ordinal())));

    @Getter
    private final Type type;

    public LogisticsSolidBlock(Type type, Properties properties) {
        // noOcclusion() is required so the BER receives a non-zero packedLight value.
        // Without it Minecraft treats the block as fully opaque, stores sky-light = 0
        // at its own position, and the BER renders pitch-black regardless of ambient light.
        super(properties.strength(6.0F).requiresCorrectToolForDrops().noOcclusion());
        this.type = type;
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos,
        boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof LogisticsSolidBlockEntity) {
            ((LogisticsSolidBlockEntity) tile).notifyOfBlockChange();
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
        BlockHitResult hitResult) {
        if (!player.isCrouching()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IGuiTileEntity guiBlockEntity) {
                if (!level.isClientSide) {
                    CoordinatesGuiProvider gp = guiBlockEntity.getGuiProvider();
                    gp.setTilePos(be).open(player);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
        ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        BlockEntity be = level.getBlockEntity(pos);
        if (level.getBlockEntity(pos) instanceof LogisticsCraftingTableTileEntity craftingTableBlockEntity) {
            craftingTableBlockEntity.placedBy(placer);
        }
        if (placer != null && be instanceof IRotationProvider rotationProvider) {
            rotationProvider.setFacing(placer.getDirection().getOpposite());
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof LogisticsSolidBlockEntity solidBlockEntity) {
                solidBlockEntity.onBlockBreak();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (!type.hasTE()) {
            return null;
        }
        return type.createTE(pos, state);
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
        // Types with a BlockEntity are drawn by LogisticsSolidBlockRenderer — suppress the
        // flat cube_all JSON model so only the 3D OBJ geometry is visible. Types without a
        // TE (frame, BC power provider) fall back to the JSON model for now.
        if (type.hasTE()) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }
        return super.getRenderShape(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(rotationProperty);
        builder.add(active);
        connectionPropertys.values().forEach(builder::add);
    }

    public enum Type {
        LOGISTICS_POWER_JUNCTION(LogisticsPowerJunctionTileEntity::new),
        LOGISTICS_SECURITY_STATION(LogisticsSecurityTileEntity::new),
        LOGISTICS_AUTOCRAFTING_TABLE(LogisticsCraftingTableTileEntity::new),
        LOGISTICS_FUZZYCRAFTING_TABLE(LogisticsCraftingTableTileEntity::new),
        LOGISTICS_STATISTICS_TABLE(LogisticsStatisticsTileEntity::new),

        // Power Provider
        LOGISTICS_RF_POWERPROVIDER(LogisticsRFPowerProviderTileEntity::new),

        LOGISTICS_PROGRAM_COMPILER(LogisticsProgramCompilerBlockEntity::new),

        LOGISTICS_BLOCK_FRAME(LogisticsFrameTileEntity::new);

        @Nullable
        private final BiFunction<BlockPos, BlockState, BlockEntity> teConstructor;
        @Getter
        final boolean hasActiveTexture;

        Type(@Nullable BiFunction<BlockPos, BlockState, BlockEntity> teConstructor) {
            this(teConstructor, false);
        }

        Type(@Nullable BiFunction<BlockPos, BlockState, BlockEntity> teConstructor, boolean hasActiveTexture) {
            this.teConstructor = teConstructor;
            this.hasActiveTexture = hasActiveTexture;
        }

        public boolean hasTE() {
            return teConstructor != null;
        }

        public BlockEntity createTE(BlockPos pos, BlockState state) {
            if (!hasTE()) {
                throw new UnsupportedOperationException("This block type has no tile entity!");
            }
            return teConstructor.apply(pos, state);
        }
    }

    // TODO: getActualState (dynamic state per neighbor) removed in 1.20.1.
    // Reimplement as a ticker that calls setChanged() + requestModelDataUpdate(),
    // or encode connection state in blockstate updates via neighborChanged().
}
