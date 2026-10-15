package logisticspipes.world.level.block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignItem;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import org.jspecify.annotations.Nullable;

import static logisticspipes.LPConstants.PIPE_MAX_POS;
import static logisticspipes.LPConstants.PIPE_MIN_POS;
import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.IRotationProvider;
import logisticspipes.interfaces.ITubeOrientation;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.CoreMultiBlockPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;
import logisticspipes.pipes.basic.LogisticsTileGenericSubMultiBlock;
import logisticspipes.ticks.QueuedTasks;
import logisticspipes.world.item.ItemLogisticsPipe;
import logisticspipes.world.level.block.entity.LPBlockEntityTypes;

public class LogisticsGenericPipeBlock extends Block implements EntityBlock {

    private static final IntegerProperty ROTATION_PROPERTY = IntegerProperty.create("rotation", 0, 3);
    private static final EnumProperty<PipeRenderModel> MODEL_TYPE_PROPERTY =
        EnumProperty.create("model_type", PipeRenderModel.class);
    private static final Map<Direction, BooleanProperty> CONNECTION_PROPERTIES = Arrays.stream(Direction.values())
        .collect(Collectors.toMap(key -> key, key -> BooleanProperty.create("connection_" + key.ordinal())));
    private static final AABB PIPE_CENTER_BB =
        new AABB(PIPE_MIN_POS, PIPE_MIN_POS, PIPE_MIN_POS, PIPE_MAX_POS, PIPE_MAX_POS, PIPE_MAX_POS);
    private static final List<AABB> PIPE_CONN_BB = Arrays.asList(
        new AABB(PIPE_MIN_POS, 0, PIPE_MIN_POS, PIPE_MAX_POS, PIPE_MIN_POS, PIPE_MAX_POS),
        new AABB(PIPE_MIN_POS, PIPE_MAX_POS, PIPE_MIN_POS, PIPE_MAX_POS, 1, PIPE_MAX_POS),
        new AABB(PIPE_MIN_POS, PIPE_MIN_POS, 0, PIPE_MAX_POS, PIPE_MAX_POS, PIPE_MIN_POS),
        new AABB(PIPE_MIN_POS, PIPE_MIN_POS, PIPE_MAX_POS, PIPE_MAX_POS, PIPE_MAX_POS, 1),
        new AABB(0, PIPE_MIN_POS, PIPE_MIN_POS, PIPE_MIN_POS, PIPE_MAX_POS, PIPE_MAX_POS),
        new AABB(PIPE_MAX_POS, PIPE_MIN_POS, PIPE_MIN_POS, 1, PIPE_MAX_POS, PIPE_MAX_POS)
    );
    /**
     * Pre-built VoxelShapes for all 64 connection combinations (one bit per Direction ordinal).
     * Index 0 = unconnected (center cube only). Built once at class-load time.
     */
    private static final VoxelShape[] PIPE_SHAPES;
    public static Map<Item, Function<Item, ? extends CoreUnroutedPipe>> PIPES = new HashMap<>();

    static {
        VoxelShape center = Shapes.create(PIPE_CENTER_BB);
        PIPE_SHAPES = new VoxelShape[64];
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = center;
            for (int i = 0; i < 6; i++) {
                if ((mask & (1 << i)) != 0) {
                    shape = Shapes.or(shape, Shapes.create(PIPE_CONN_BB.get(i)));
                }
            }
            PIPE_SHAPES[mask] = shape;
        }
    }

    /**
     * The shape depends on the block entity, so it must not be cached per block state: the cache
     * is built without one and would see a full cube, making every face of a thin pipe sturdy
     * enough to hold a torch. Without the cache the state counts as non-solid, though, which lets
     * buckets and flowing fluids replace the pipe, so it is forced solid.
     */
    public LogisticsGenericPipeBlock(Properties properties) {
        super(properties.strength(1.5F).noOcclusion().dynamicShape().forceSolidOn()
            .isViewBlocking((state, world, pos) -> false)
            .isSuffocating((state, world, pos) -> false)
            .isRedstoneConductor((state, world, pos) -> false));
        registerDefaultState(this.stateDefinition.any()
            .setValue(ROTATION_PROPERTY, 0)
            .setValue(MODEL_TYPE_PROPERTY, PipeRenderModel.NONE));
    }

    public static void removePipe(CoreUnroutedPipe pipe) {
        if (!LogisticsGenericPipeBlock.isValid(pipe)) {
            return;
        }

        if (pipe.canBeDestroyed() || pipe.destroyByPlayer()) {
            pipe.onBlockRemoval();
        } else if (pipe.preventRemove()) {
            LogisticsGenericPipeBlock.cacheTileToPreventRemoval(pipe);
        }

        Level level = Objects.requireNonNull(pipe.getContainer()).getLevel();
        Objects.requireNonNull(level);

        if (pipe.isMultiBlock()) {
            if (pipe.preventRemove()) {
                throw new UnsupportedOperationException("A multi block can't be protected against removal.");
            }
            final BlockPos mainPos = pipe.getPos();
            List<CoreMultiBlockPipe.SubBlock> list = ((CoreMultiBlockPipe) pipe).getRotatedSubBlocks();
            for (BlockPos pos : pipe.getContainer().subMultiBlock) {
                BlockEntity tile = level.getBlockEntity(pos);
                if (tile instanceof LogisticsTileGenericSubMultiBlock multiBlock) {
                    list.stream()
                        .min(Comparator.comparingDouble(block -> block.at(mainPos).distSqr(pos)))
                        .ifPresent(equ -> multiBlock.removeSubType(equ.type()));
                    if (multiBlock.removeMainPipe(pipe.getPos())) {
                        LogisticsGenericSubMultiBlockBlock.redirectedToMainPipe = true;
                        level.removeBlock(pos, false);
                        LogisticsGenericSubMultiBlockBlock.redirectedToMainPipe = false;
                    } else {
                        TargetLookup.sendToChunkWatchers(tile, multiBlock.getDescriptionMessage());
                    }
                }
            }
        }

        level.removeBlockEntity(pipe.getContainer().getBlockPos());
    }

    /**
     * Registers a pipe item via DeferredRegister and adds it to {@link #PIPES}.
     * The registry name will be {@code pipe_<name>}.
     */
    public static DeferredItem<ItemLogisticsPipe> registerPipe(
        DeferredRegister.Items registry,
        String name,
        Function<Item, ? extends CoreUnroutedPipe> constructor) {
        return registry.registerItem("pipe_" + name, properties -> {
            ItemLogisticsPipe item = new ItemLogisticsPipe(properties);
            LogisticsGenericPipeBlock.PIPES.put(item, constructor);
            // A pipe instance per item type, for queries made before a real pipe exists:
            // placement in ItemLogisticsPipe.useOn and the item renderer.
            try {
                item.setDummyPipe(constructor.apply(item));
            } catch (Exception e) {
                LogisticsPipes.LOG.error("Failed to create dummy pipe for {}", name, e);
            }
            return item;
        });
    }

    @Nullable
    public static CoreUnroutedPipe createPipe(Item key) {
        Function<Item, ? extends CoreUnroutedPipe> pipe = LogisticsGenericPipeBlock.PIPES.get(key);
        if (pipe != null) {
            return pipe.apply(key);
        } else {
            LogisticsPipes.LOG.warn("Detected pipe with unknown key ({}). This should not have happen.", key);
        }

        return null;
    }

    public static boolean placePipe(CoreUnroutedPipe pipe, Level level, BlockPos blockPos, Block block) {
        return LogisticsGenericPipeBlock.placePipe(pipe, level, blockPos, block, null);
    }

    public static boolean placePipe(CoreUnroutedPipe pipe, Level level, BlockPos blockPos, Block block,
        @Nullable ITubeOrientation orientation) {
        BlockState oldBlockState = level.getBlockState(blockPos);
        boolean placed = level.setBlock(blockPos, block.defaultBlockState(), 3);

        if (level.isClientSide()) {
            return placed;
        }

        if (placed) {
            BlockEntity tile = level.getBlockEntity(blockPos);
            if (tile instanceof LogisticsGenericPipeBlockEntity tilePipe) {
                if (pipe instanceof CoreMultiBlockPipe mPipe) {
                    if (orientation == null) {
                        throw new NullPointerException();
                    }
                    orientation.setOnPipe(mPipe);
                    LogisticsGenericSubMultiBlockBlock.CURRENT_CREATED_MULTI_BLOCK = blockPos;
                    for (CoreMultiBlockPipe.SubBlock sub : mPipe.getRotatedSubBlocks()) {
                        final BlockPos subPos = sub.at(blockPos);
                        BlockEntity subTile = level.getBlockEntity(subPos);
                        BlockState oldSubBlockState = level.getBlockState(subPos);
                        if (subTile instanceof LogisticsTileGenericSubMultiBlock multiBlock) {
                            multiBlock.addMultiBlockMainPos(blockPos);
                            multiBlock.addSubTypeTo(sub.type());
                            TargetLookup.sendToChunkWatchers(subTile, multiBlock.getDescriptionMessage());
                        } else {
                            level.setBlock(subPos, LPBlocks.SUB_MULTIBLOCK.get().defaultBlockState(), 3);
                            subTile = level.getBlockEntity(subPos);
                            if (subTile instanceof LogisticsTileGenericSubMultiBlock multiBlock) {
                                multiBlock.addSubTypeTo(sub.type());
                            }
                        }
                        level.markAndNotifyBlock(subPos, level.getChunkAt(subPos), oldSubBlockState,
                            level.getBlockState(subPos), 3, 512);
                    }
                    LogisticsGenericSubMultiBlockBlock.CURRENT_CREATED_MULTI_BLOCK = null;
                }
                tilePipe.initialize(pipe);
            }
            level.markAndNotifyBlock(blockPos, level.getChunkAt(blockPos), oldBlockState, level.getBlockState(blockPos),
                Block.UPDATE_ALL, 512);
        }

        return placed;
    }

    @Nullable
    public static CoreUnroutedPipe getPipe(BlockGetter blockAccess, BlockPos pos) {
        BlockEntity tile = blockAccess.getBlockEntity(pos);

        if (!(tile instanceof LogisticsGenericPipeBlockEntity genericPipe) || tile.isRemoved()) {
            return null;
        } else {
            return genericPipe.pipe;
        }
    }

    public static boolean isFullyDefined(@Nullable CoreUnroutedPipe pipe) {
        return pipe != null && pipe.transport != null && pipe.getContainer() != null;
    }

    public static boolean isValid(@Nullable CoreUnroutedPipe pipe) {
        return LogisticsGenericPipeBlock.isFullyDefined(pipe);
    }

    private static void cacheTileToPreventRemoval(CoreUnroutedPipe pipe) {
        final Level worldCache = pipe.getLevel();
        final BlockPos posCache = pipe.getPos();
        final BlockEntity tileCache = pipe.getContainer();
        final CoreUnroutedPipe fPipe = pipe;
        fPipe.setPreventRemove(true);
        QueuedTasks.queueTask(() -> {
            if (!fPipe.preventRemove()) {
                return null;
            }
            boolean changed = false;
            if (!worldCache.getBlockState(posCache).is(LPBlocks.PIPE)) {
                worldCache.setBlock(posCache, LPBlocks.PIPE.get().defaultBlockState(), 3);
                changed = true;
            }
            if (worldCache.getBlockEntity(posCache) != tileCache) {
                worldCache.setBlockEntity(tileCache);
                changed = true;
            }
            if (changed) {
                worldCache.markAndNotifyBlock(posCache, worldCache.getChunkAt(posCache),
                    worldCache.getBlockState(posCache), worldCache.getBlockState(posCache), 3, 512);
            }
            fPipe.setPreventRemove(false);
            return null;
        });
    }

    /**
     * Block-local VoxelShape of a multiblock tube for one block cell, built from the pipe's
     * world-space collision boxes ({@link CoreMultiBlockPipe#addCollisionBoxesToList}).
     * Returns {@link Shapes#empty()} when no box touches the cell.
     */
    public static VoxelShape getMultiBlockShape(CoreMultiBlockPipe pipe, BlockPos cell) {
        List<AABB> boxes = new ArrayList<>();
        pipe.addCollisionBoxesToList(boxes, new AABB(cell));
        VoxelShape shape = Shapes.empty();
        for (AABB box : boxes) {
            shape = Shapes.or(shape, Shapes.create(box.move(-cell.getX(), -cell.getY(), -cell.getZ())));
        }
        return shape;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION_PROPERTY, MODEL_TYPE_PROPERTY);
        CONNECTION_PROPERTIES.values().forEach(builder::add);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogisticsGenericPipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
        BlockEntityType<T> type) {
        return BaseEntityBlock.createTickerHelper(type, LPBlockEntityTypes.PIPE.get(),
            level.isClientSide() ? LogisticsGenericPipeBlockEntity::clientTick : LogisticsGenericPipeBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // MODEL puts the pipe in the chunk mesh, where PipeBakedModel supplies the geometry
        // from the ModelData the block entity snapshots.
        return RenderShape.MODEL;
    }

    /**
     * The pipe item plus whatever was traveling through it. There is no loot table: the drops
     * depend on the pipe in the block entity, which the caller captured before removing the block,
     * so it is still here after {@link #removePipe} has torn the pipe down.
     */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity blockEntityParameter = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(blockEntityParameter instanceof LogisticsGenericPipeBlockEntity blockEntity)) {
            return List.of();
        }
        CoreUnroutedPipe pipe = blockEntity.pipe;
        if (pipe == null || !(pipe.canBeDestroyed() || pipe.destroyByPlayer())) {
            return List.of();
        }
        List<ItemStack> drops = new ArrayList<>(pipe.dropContents());
        drops.add(new ItemStack(pipe.item));
        return drops;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (!(world.getBlockEntity(pos) instanceof LogisticsGenericPipeBlockEntity blockEntity)) {
            return Shapes.block();
        }
        CoreUnroutedPipe pipe = blockEntity.pipe;
        if (pipe == null || pipe.isPipeBlock()) {
            return Shapes.block();
        }
        if (pipe.isMultiBlock() && !pipe.actAsNormalPipe()) {
            // The pipe center stays included so the main block is always targetable.
            return Shapes.or(Shapes.create(PIPE_CENTER_BB), getMultiBlockShape((CoreMultiBlockPipe) pipe, pos));
        }
        int mask = 0;
        for (Direction dir : Direction.values()) {
            if (blockEntity.isPipeConnectedCached(dir)) {
                mask |= (1 << dir.ordinal());
            }
        }
        return PIPE_SHAPES[mask];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return getShape(state, world, pos, context);
    }

    /**
     * Pipes never attenuate light.
     *
     * <p>Both of these have to be overridden because the default implementations derive the
     * answer from {@link #getShape}, and the light engine does not ask per position: the value
     * is computed once per {@link BlockState} in {@code BlockBehaviour.BlockStateBase.initCache},
     * with an {@code EmptyBlockGetter} and {@code BlockPos.ZERO}. There is no block entity in
     * that context, so {@code getShape} takes its no-tile fallback and returns
     * {@link Shapes#block()} — a full cube. The default {@code propagatesSkylightDown} then
     * reports false and {@code getLightBlock} caches 1, so every pipe dimmed the light passing
     * through it and a block enclosed by pipes went dark from all sides.</p>
     *
     * <p>Fixing {@code getShape}'s fallback instead would be wrong: it is a full cube on
     * purpose, so an unloaded or pipe-block tile stays targetable and collidable.</p>
     */
    @Override
    protected int getLightDampening(BlockState state) {
        return 0;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData,
        Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof LogisticsGenericPipeBlockEntity genericPipe &&
            LogisticsGenericPipeBlock.isValid(genericPipe.pipe)) {
            return new ItemStack(genericPipe.pipe.item);
        }
        return super.getCloneItemStack(level, pos, state, includeData, player);
    }

    /* Wrappers ************************************************************ */
    @Override
    protected void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn,
        @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, worldIn, pos, blockIn, orientation, movedByPiston);

        CoreUnroutedPipe pipe = LogisticsGenericPipeBlock.getPipe(worldIn, pos);

        if (LogisticsGenericPipeBlock.isValid(pipe)) {
            pipe.getContainer().scheduleNeighborChange();
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
        ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        CoreUnroutedPipe pipe = LogisticsGenericPipeBlock.getPipe(level, pos);

        if (LogisticsGenericPipeBlock.isValid(pipe)) {
            pipe.onBlockPlaced();
            pipe.onBlockPlacedBy(placer);
            if (pipe instanceof IRotationProvider) {
                ((IRotationProvider) pipe).setFacing(placer.getDirection().getOpposite());
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
        BlockHitResult hitResult) {
        InteractionResult superResult = super.useWithoutItem(state, level, pos, player, hitResult);
        if (superResult != InteractionResult.PASS) {
            return superResult;
        }
        ItemStack heldItem = player.getInventory().getItem(player.getInventory().getSelectedSlot());
        CoreUnroutedPipe pipe = LogisticsGenericPipeBlock.getPipe(level, pos);
        if (LogisticsGenericPipeBlock.isValid(pipe)) {
            if (!heldItem.isEmpty()) {
                if (heldItem.getItem() instanceof SignItem) {
                    // Sign will be placed anyway, so lets show the sign gui
                    return InteractionResult.PASS;
                } else if (heldItem.getItem() instanceof ItemLogisticsPipe) {
                    return InteractionResult.PASS;
                }
            }
            return pipe.blockActivated(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    public enum PipeRenderModel implements StringRepresentable {
        NONE,
        REQUEST_TABLE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }
}
