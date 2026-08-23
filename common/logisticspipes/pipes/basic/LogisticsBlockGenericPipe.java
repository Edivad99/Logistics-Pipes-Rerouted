package logisticspipes.pipes.basic;

import static logisticspipes.LPConstants.PIPE_MAX_POS;
import static logisticspipes.LPConstants.PIPE_MIN_POS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import logisticspipes.world.level.block.LPBlocks;
import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.IRotationProvider;
import logisticspipes.interfaces.ITickable;
import logisticspipes.interfaces.ITubeOrientation;
import logisticspipes.world.item.ItemLogisticsPipe;
import logisticspipes.pipes.basic.ltgpmodcompat.LPMicroblockBlock;
import logisticspipes.proxy.MainProxy;
import logisticspipes.ticks.QueuedTasks;
import logisticspipes.utils.LPPositionSet;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import network.rs485.logisticspipes.world.DoubleCoordinates;
import network.rs485.logisticspipes.world.DoubleCoordinatesType;
import net.minecraft.world.level.redstone.Orientation;

// BlockStateContainer removed — use StateDefinition.Builder in createBlockStateDefinition()
// Particle/ParticleEngine/TextureAtlasSprite imports removed — rendering deferred (see addHitEffects/addDestroyEffects TODOs)
// ClientConfiguration import removed — used only in deferred rendering methods

public class LogisticsBlockGenericPipe extends LPMicroblockBlock {

	public static boolean ignoreSideRayTrace = false;
	public static Map<Item, Function<Item, ? extends CoreUnroutedPipe>> pipes = new HashMap<>();
	public static Map<DoubleCoordinates, CoreUnroutedPipe> pipeRemoved = new HashMap<>();
	public static Map<DoubleCoordinates, BlockPos> pipeSubMultiRemoved = new HashMap<>();
	private static long lastRemovedDate = -1;

    public static final IntegerProperty rotationProperty = IntegerProperty.create("rotation", 0, 3);
	public static final EnumProperty<PipeRenderModel> modelTypeProperty = EnumProperty.create("model_type", PipeRenderModel.class);
	public static final Map<Direction, BooleanProperty> connectionPropertys = Arrays.stream(Direction.values()).collect(Collectors
			.toMap(key -> key, key -> BooleanProperty.create("connection_" + key.ordinal())));


	public static final AABB PIPE_CENTER_BB = new AABB(PIPE_MIN_POS, PIPE_MIN_POS, PIPE_MIN_POS, PIPE_MAX_POS, PIPE_MAX_POS, PIPE_MAX_POS);
	public static final List<AABB> PIPE_CONN_BB = Arrays.asList(
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

	public enum PipeRenderModel implements StringRepresentable {
		NONE,
		REQUEST_TABLE;

		@Override
        public String getSerializedName() {
			return name().toLowerCase();
		}
	}

	public LogisticsBlockGenericPipe(Properties properties) {
		super(properties.strength(1.5F).noOcclusion()
				.isViewBlocking((state, world, pos) -> false)
				.isSuffocating((state, world, pos) -> false)
				.isRedstoneConductor((state, world, pos) -> false));
		registerDefaultState(this.stateDefinition.any()
				.setValue(rotationProperty, 0)
				.setValue(modelTypeProperty, PipeRenderModel.NONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(rotationProperty, modelTypeProperty);
		connectionPropertys.values().forEach(builder::add);
		// TODO: propertyRenderList / propertyCache were IExtendedBlockState properties — removed in 1.20.1; rendering rewrite needed
	}

    @Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LogisticsTileGenericPipe(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
			Level level,
			BlockState state,
			BlockEntityType<T> type) {
		// Without a ticker registered on the owning block, BlockEntity.tick equivalents
		// (here: LogisticsTileGenericPipe.update via ITickable) are never called and the
		// entire mod — routing graph updates, module logic, item transport — sits idle.
		return (lvl, pos, st, be) -> {
			if (be instanceof ITickable tickable) tickable.update();
		};
	}

	@Override
    public RenderShape getRenderShape(BlockState state) {
		// MODEL puts the pipe in the chunk mesh, where PipeBakedModel supplies the geometry
		// from the ModelData the block entity snapshots.
		return RenderShape.MODEL;
	}

	public static void removePipe(CoreUnroutedPipe pipe) {
		if (!LogisticsBlockGenericPipe.isValid(pipe)) {
			return;
		}

		if (pipe.canBeDestroyed() || pipe.destroyByPlayer()) {
			pipe.onBlockRemoval();
		} else if (pipe.preventRemove()) {
			LogisticsBlockGenericPipe.cacheTileToPreventRemoval(pipe);
		}

		Level level = pipe.container.getLevel();

		if (LogisticsBlockGenericPipe.lastRemovedDate != level.getGameTime()) {
			LogisticsBlockGenericPipe.lastRemovedDate = level.getGameTime();
			LogisticsBlockGenericPipe.pipeRemoved.clear();
			LogisticsBlockGenericPipe.pipeSubMultiRemoved.clear();
		}

		if (pipe.isMultiBlock()) {
			if (pipe.preventRemove()) {
				throw new UnsupportedOperationException("A multi block can't be protected against removal.");
			}
			LPPositionSet<DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare>> list = ((CoreMultiBlockPipe) pipe).getRotatedSubBlocks();
			list.forEach(pos -> pos.add(new DoubleCoordinates(pipe)));
			for (DoubleCoordinates pos : pipe.container.subMultiBlock) {
				BlockEntity tile = pos.getTileEntity(level);
				if (tile instanceof LogisticsTileGenericSubMultiBlock) {
					DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare> equ = list.findClosest(pos);
					if (equ != null) {
						((LogisticsTileGenericSubMultiBlock) tile).removeSubType(equ.getType());
					}
					if (((LogisticsTileGenericSubMultiBlock) tile).removeMainPipe(new DoubleCoordinates(pipe))) {
						LogisticsBlockGenericSubMultiBlock.redirectedToMainPipe = true;
						pos.setBlockToAir(level);
						LogisticsBlockGenericSubMultiBlock.redirectedToMainPipe = false;
						LogisticsBlockGenericPipe.pipeSubMultiRemoved.put(new DoubleCoordinates(pos), pipe.container.getBlockPos());
					} else {
						MainProxy.sendPacketToAllWatchingChunk(tile, ((LogisticsTileGenericSubMultiBlock) tile).getLPDescriptionPacket());
					}
				}
			}
		}

		BlockPos pos = pipe.container.getBlockPos();
		LogisticsBlockGenericPipe.pipeRemoved.put(new DoubleCoordinates(pos), pipe);
		level.removeBlockEntity(pos);
	}

	/* Registration ******************************************************** */

	/**
	 * Registers a pipe item via DeferredRegister and adds it to the {@code pipes} map.
	 * The registry name will be {@code pipe_<name>}.
	 *
	 * <p>Icon/dummy-pipe setup (setPipeIconIndex, setIconProviderFromPipe, setDummyPipe)
	 * is intentionally deferred — those methods belong to the 1.12.2 rendering system
	 * and will be addressed when ItemLogisticsPipe is migrated to 1.20.1.</p>
	 */
	public static DeferredItem<ItemLogisticsPipe> registerPipe(
			DeferredRegister.Items registry,
			String name,
			Function<Item, ? extends CoreUnroutedPipe> constructor) {
		return registry.registerItem("pipe_" + name, properties -> {
			ItemLogisticsPipe item = new ItemLogisticsPipe(properties);
			LogisticsBlockGenericPipe.pipes.put(item, constructor);
			// Create a dummy pipe instance for type/size queries (isMultiBlock, etc.)
			// used by ItemLogisticsPipe.useOn before a real pipe is placed.
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
		Function<Item, ? extends CoreUnroutedPipe> pipe = LogisticsBlockGenericPipe.pipes.get(key);
		if (pipe != null) {
			return pipe.apply(key);
		} else {
			LogisticsPipes.LOG.warn("Detected pipe with unknown key (" + key + "). This should not have happend.");
		}

		return null;
	}

	public static boolean placePipe(CoreUnroutedPipe pipe, Level level, BlockPos blockPos, Block block) {
		return LogisticsBlockGenericPipe.placePipe(pipe, level, blockPos, block, null);
	}

	public static boolean placePipe(CoreUnroutedPipe pipe, Level level, BlockPos blockPos, Block block, ITubeOrientation orientation) {
		BlockState oldBlockState = level.getBlockState(blockPos);
		boolean placed = level.setBlock(blockPos, block.defaultBlockState(), 3);

		if (level.isClientSide()) {
			return placed;
		}

		if (placed) {
			BlockEntity tile = level.getBlockEntity(blockPos);
			if (tile instanceof LogisticsTileGenericPipe) {
				LogisticsTileGenericPipe tilePipe = (LogisticsTileGenericPipe) tile;
				if (pipe instanceof CoreMultiBlockPipe) {
					if (orientation == null) {
						throw new NullPointerException();
					}
					CoreMultiBlockPipe mPipe = (CoreMultiBlockPipe) pipe;
					orientation.setOnPipe(mPipe);
					DoubleCoordinates placeAt = new DoubleCoordinates(blockPos);
					LogisticsBlockGenericSubMultiBlock.currentCreatedMultiBlock = placeAt;
					LPPositionSet<DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare>> positions = ((CoreMultiBlockPipe) pipe).getSubBlocks();
					orientation.rotatePositions(positions);
					for (DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare> pos : positions) {
						pos.add(placeAt);
						BlockEntity subTile = level.getBlockEntity(pos.getBlockPos());
						BlockState oldSubBlockState = level.getBlockState(pos.getBlockPos());
						if (subTile instanceof LogisticsTileGenericSubMultiBlock) {
							((LogisticsTileGenericSubMultiBlock) subTile).addMultiBlockMainPos(placeAt);
							((LogisticsTileGenericSubMultiBlock) subTile).addSubTypeTo(pos.getType());
							MainProxy.sendPacketToAllWatchingChunk(subTile, ((LogisticsTileGenericSubMultiBlock) subTile).getLPDescriptionPacket());
						} else {
							level.setBlock(pos.getBlockPos(), LPBlocks.SUB_MULTIBLOCK.get().defaultBlockState(), 3);
							subTile = level.getBlockEntity(pos.getBlockPos());
							if (subTile instanceof LogisticsTileGenericSubMultiBlock) {
								((LogisticsTileGenericSubMultiBlock) subTile).addSubTypeTo(pos.getType());
							}
						}
						level.markAndNotifyBlock(pos.getBlockPos(), level.getChunkAt(pos.getBlockPos()), oldSubBlockState, level.getBlockState(pos.getBlockPos()), 3, 512);
					}
					LogisticsBlockGenericSubMultiBlock.currentCreatedMultiBlock = null;
				}
				tilePipe.initialize(pipe);
				//				tilePipe.sendUpdateToClient();
			}
			level.markAndNotifyBlock(blockPos, level.getChunkAt(blockPos), oldBlockState, level.getBlockState(blockPos), 3, 512);
		}

		return placed;
	}

    @Nullable
	public static CoreUnroutedPipe getPipe(BlockGetter blockAccess, BlockPos pos) {
		BlockEntity tile = blockAccess.getBlockEntity(pos);

		if (!(tile instanceof LogisticsTileGenericPipe) || tile.isRemoved()) {
			return null;
		} else {
			return ((LogisticsTileGenericPipe) tile).pipe;
		}
	}

	public static boolean isFullyDefined(@Nullable CoreUnroutedPipe pipe) {
		return pipe != null && pipe.transport != null && pipe.container != null;
	}

	public static boolean isValid(@Nullable CoreUnroutedPipe pipe) {
		return LogisticsBlockGenericPipe.isFullyDefined(pipe);
	}

	private static void cacheTileToPreventRemoval(CoreUnroutedPipe pipe) {
		final Level worldCache = pipe.getWorld();
		final BlockPos posCache = pipe.getPos();
		final BlockEntity tileCache = pipe.container;
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
				worldCache.markAndNotifyBlock(posCache, worldCache.getChunkAt(posCache), worldCache.getBlockState(posCache), worldCache.getBlockState(posCache), 3, 512);
			}
			fPipe.setPreventRemove(false);
			return null;
		});
	}

	// @Override removed — getDrops(BlockGetter...) does not match 1.20.1 Block API
	public NonNullList<ItemStack> getDrops(BlockGetter world, BlockPos pos, BlockState state, int fortune) {
		NonNullList<ItemStack> list = NonNullList.create();
		if (world instanceof Level && MainProxy.isClient((Level) world)) {
			return list;
		}

		int count = 1; // quantityDropped removed in 1.20.1
		for (int i = 0; i < count; i++) {
			CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.getPipe(world, pos);

			if (pipe == null) {
				pipe = LogisticsBlockGenericPipe.pipeRemoved.get(new DoubleCoordinates(pos));
			}

			if (pipe != null) {
				if (pipe.item != null && (pipe.canBeDestroyed() || pipe.destroyByPlayer())) {
					list.addAll(pipe.dropContents());
					list.add(new ItemStack(pipe.item, 1));
				} else if (pipe.item != null) {
					LogisticsBlockGenericPipe.cacheTileToPreventRemoval(pipe);
				}
			}
		}
		return list;
	}

	// getBlockFaceShape removed in 1.20.1; dead stub kept for reference
	public Object /* BlockFaceShape */ getBlockFaceShape_DEAD(BlockGetter worldIn, BlockState state, BlockPos pos, Direction face) {
		return null; // BlockFaceShape.UNDEFINED — removed in 1.20.1
	}

	public void addCollisionBoxToList(LogisticsTileGenericPipe pipe, AABB entityBox, List<AABB> collidingBoxes, Entity entityIn, boolean isActualState) {
		addCollisionBoxToList(pipe.getLevel().getBlockState(pipe.getBlockPos()), pipe.getLevel(), pipe.getBlockPos(), entityBox, collidingBoxes, entityIn, isActualState);
	}

	/** Replaces removed Block.addCollisionBoxToList — offsets the box by pos, then adds to list if it intersects entityBox. */
	private static void addCollisionBoxToList(BlockPos pos, AABB entityBox, List<AABB> collidingBoxes, AABB box) {
		AABB offsetBox = box.move(pos.getX(), pos.getY(), pos.getZ());
		if (entityBox.intersects(offsetBox)) {
			collidingBoxes.add(offsetBox);
		}
	}

	// Internal collision helper — addCollisionBoxToList is no longer an MC override in 1.20.1; called from the overload above
	public void addCollisionBoxToList(BlockState state, Level level, BlockPos pos, AABB entityBox, List<AABB> collidingBoxes, @Nullable Entity entity, boolean isActualState) {
		BlockEntity te = level.getBlockEntity(pos);
		if (te instanceof LogisticsTileGenericPipe) {
			LogisticsTileGenericPipe tile = (LogisticsTileGenericPipe) te;
			CoreUnroutedPipe pipe = tile.pipe;
			if (pipe != null && pipe.isPipeBlock()) {
				addCollisionBoxToList(pos, entityBox, collidingBoxes, new AABB(0, 0, 0, 1, 1, 1)); // Block.FULL_BLOCK_AABB removed in 1.20.1
				return;
			}
			if (pipe != null && pipe.isMultiBlock()) {
				((CoreMultiBlockPipe) pipe).addCollisionBoxesToList(collidingBoxes, entityBox);
				if (!pipe.actAsNormalPipe()) return;
			}

			Arrays.stream(Direction.values())
					.filter(tile::isPipeConnectedCached)
					.map(f -> PIPE_CONN_BB.get(f.ordinal()))
					.forEach(bb -> addCollisionBoxToList(pos, entityBox, collidingBoxes, bb));
		}
		addCollisionBoxToList(pos, entityBox, collidingBoxes, PIPE_CENTER_BB);
	}

	@Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		BlockEntity te = world.getBlockEntity(pos);
		if (!(te instanceof LogisticsTileGenericPipe tile)) {
            return Shapes.block();
        }
        CoreUnroutedPipe pipe = tile.pipe;
		if (pipe == null || pipe.isPipeBlock()) {
            return Shapes.block();
        }
		if (pipe.isMultiBlock() && !pipe.actAsNormalPipe()) {
			// The pipe centre stays included so the main block is always targetable,
			// matching doRayTraceMultiblock's centre-box test.
			return Shapes.or(Shapes.create(PIPE_CENTER_BB), getMultiBlockShape((CoreMultiBlockPipe) pipe, pos));
		}
		int mask = 0;
		for (Direction dir : Direction.values()) {
			if (tile.isPipeConnectedCached(dir)) {
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
	protected int getLightBlock(BlockState state) {
		return 0;
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return true;
	}

    /**
	 * Block-local VoxelShape of a multiblock tube for one block cell, built from the pipe's
	 * world-space collision boxes (the same geometry LP1 fed to addCollisionBoxToList).
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

// collisionRayTrace removed in 1.20.1 — migrated to getShape(BlockState, BlockGetter, BlockPos, CollisionContext)
	// The entire ray-trace API (AABB.calculateIntercept, new HitResult(hitVec, side, pos), Block.FULL_BLOCK_AABB) is gone.
	// @Override
	// public HitResult collisionRayTrace(BlockState state, Level world, BlockPos pos, Vec3 start, Vec3 end) { ... }

	public InternalRayTraceResult doRayTrace(Level level, BlockPos pos, Player player) {
		double reachDistance = player instanceof ServerPlayer
				? player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE)
				: 5;

		Vec3 lookVec = player.getLookAngle();
		Vec3 start = player.getEyePosition();
		Vec3 end = start.add(lookVec.x * reachDistance, lookVec.y * reachDistance, lookVec.z * reachDistance);

		return doRayTrace(level, pos, start, end);
	}

    @Nullable
	public InternalRayTraceResult doRayTrace(Level level, BlockPos pos, Vec3 start, Vec3 end) {
		BlockEntity te = level.getBlockEntity(pos);

		if (te instanceof LogisticsTileGenericPipe) {
			LogisticsTileGenericPipe tileG = (LogisticsTileGenericPipe) te;
			CoreUnroutedPipe pipe = tileG.pipe;
			if (!LogisticsBlockGenericPipe.isValid(pipe)) return null;

			if (pipe.isMultiBlock()) {
				InternalRayTraceResult result1 = doRayTraceMultiblock(tileG, (CoreMultiBlockPipe) pipe, start, end);

				if (!pipe.actAsNormalPipe())
					return result1;

				InternalRayTraceResult result2 = doRayTrace(tileG, pipe, start, end);

				return Stream.of(result1, result2)
						.filter(Objects::nonNull)
						.min(Comparator.comparing(r -> r.rayTraceResult.getLocation().distanceToSqr(start)))
						.orElse(null);
			} else {
				return doRayTrace(tileG, pipe, start, end);
			}
		}
		return null;
	}

	@Data
	@AllArgsConstructor
	private static class Hit {

		public HitResult rayTraceResult;
		public AABB box;
		public Direction side;
		public Part part;
	}

    @Nullable
	private InternalRayTraceResult doRayTrace(LogisticsTileGenericPipe tileG, CoreUnroutedPipe pipe, Vec3 start, Vec3 end) {
		if (tileG == null) return null;
		if (!LogisticsBlockGenericPipe.isValid(pipe)) return null;

		/*
		 * pipe hits along x, y, and z axis, gate (all 6 sides) [and
		 * wires+facades]
		 */
		ArrayList<Hit> list = new ArrayList<>();

		// pipe
		for (Direction side : LogisticsBlockGenericPipe.DIR_VALUES) {
			if (side == null || tileG.isPipeConnectedCached(side)) {
				if (side != null && ignoreSideRayTrace) continue;
				AABB bb = getPipeBoundingBox(side);
				// rayTrace(pos, start, end, AABB) removed in 1.20.1 — use VoxelShape.clip
				list.add(new Hit(Shapes.create(bb).clip(start, end, tileG.getBlockPos()), bb, side, Part.PIPE));
			}
		}

		// pluggables

		/*
		for (Direction side : Direction.values()) {
			if (tileG.getBCPipePluggable(side) != null) {
				if(side != null && ignoreSideRayTrace) continue;
				AABB bb = tileG.getBCPipePluggable(side).getBoundingBox(side);
				boxes[7 + side.ordinal()] = bb;
				hits[7 + side.ordinal()] = super.collisionRayTrace(new BoundingBoxDelegateBlockState(bb, state), tileG.getLevel(), tileG.getBlockPos(), start, end);
				sideHit[7 + side.ordinal()] = side;
			}
		}
		*/

		// wire hit-test not implemented

		// get closest hit

		return list.stream()
				.filter(r -> r.rayTraceResult != null)
				.min(Comparator.comparing(r -> r.rayTraceResult.getLocation().distanceToSqr(start)))
				.map(r -> new InternalRayTraceResult(r.part, r.rayTraceResult, r.box, r.side))
				.orElse(null);
	}

	/**
	 * Ray-trace against a multi-block pipe's central bounding box plus the bounding box of each
	 * sub-block position (translated from the main tile's position). Uses {@link Shapes#create}
	 * and {@link VoxelShape#clip} since the 1.12
	 * {@code AABB.calculateIntercept} API is gone.
	 */
	@Nullable
    private InternalRayTraceResult doRayTraceMultiblock(LogisticsTileGenericPipe tileG, CoreMultiBlockPipe pipe, Vec3 start, Vec3 end) {
		ArrayList<Hit> list = new ArrayList<>();
		// Centre block — always test so the player can click the main pipe body itself.
		BlockPos mainPos = tileG.getBlockPos();
		AABB centerBox = PIPE_CENTER_BB.move(mainPos.getX(), mainPos.getY(), mainPos.getZ());
		BlockHitResult centerHit =
				Shapes.create(centerBox).clip(start, end, mainPos);
		if (centerHit != null) {
			list.add(new Hit(centerHit, centerBox, null, Part.PIPE));
		}

		// Test the tube's real world-space collision boxes so targeting matches the
		// visible geometry instead of full cubes at each sub-block position.
		List<AABB> tubeBoxes = new ArrayList<>();
		pipe.addCollisionBoxesToList(tubeBoxes, null);
		for (AABB box : tubeBoxes) {
			BlockPos cell = BlockPos.containing(box.getCenter());
			BlockHitResult subHit = Shapes
					.create(box.move(-cell.getX(), -cell.getY(), -cell.getZ()))
					.clip(start, end, cell);
			if (subHit != null) {
				list.add(new Hit(subHit, box.move(-mainPos.getX(), -mainPos.getY(), -mainPos.getZ()), null, Part.PIPE));
			}
		}

		return list.stream()
				.filter(r -> r.rayTraceResult != null)
				.min(Comparator.comparing(r -> r.rayTraceResult.getLocation().distanceToSqr(start)))
				.map(r -> new InternalRayTraceResult(r.part, r.rayTraceResult, r.box, r.side))
				.orElse(null);
	}

	private AABB getPipeBoundingBox(@Nullable Direction side) {
		if (side == null) return PIPE_CENTER_BB;
		return PIPE_CONN_BB.get(side.ordinal());
	}

	// createNewTileEntity removed in 1.20.1 — replaced by newBlockEntity(BlockPos, BlockState) above

	public enum Part {
		PIPE,
		UNKNOWN
	}

	public static class InternalRayTraceResult {

		public final Part hitPart;
		public final HitResult rayTraceResult;
		public final AABB boundingBox;
		public final Direction sideHit;

		InternalRayTraceResult(Part hitPart, HitResult rayTraceResult, AABB boundingBox, Direction side) {
			this.hitPart = hitPart;
			this.rayTraceResult = rayTraceResult;
			this.boundingBox = boundingBox;
			sideHit = side;
		}

		@Override
		public String toString() {
			return String.format("HitResult: %s, %s", hitPart == null ? "null" : hitPart.name(), boundingBox == null ? "null" : boundingBox.toString());
		}
	}

	private static final Direction[] DIR_VALUES;

	static {
		DIR_VALUES = new Direction[Direction.values().length + 1];
		DIR_VALUES[0] = null;
		System.arraycopy(Direction.values(), 0, DIR_VALUES, 1, Direction.values().length);
	}

	// getBlockHardness removed in 1.20.1 — set via BlockBehaviour.Properties.strength() in constructor
	// getRenderType removed in 1.20.1 — MODEL is the default
	// getRenderLayer removed in 1.20.1 — use ItemBlockRenderTypes.setRenderLayer in client setup
	// isFullBlock / isFullCube / isNormalCube / isOpaqueCube / isTopSolid — all removed in 1.20.1

	// @Override removed — canBeReplacedByLeaves removed in 1.20.1
	public boolean canBeReplacedByLeaves_DEAD(BlockState state, BlockGetter world, BlockPos pos) {
		return false;
	}

	// @Override removed — isSideSolid(BlockGetter...) removed in 1.20.1
	public boolean isSideSolid_DEAD(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
		BlockEntity tile = world.getBlockEntity(pos);

		if (tile instanceof LogisticsTileGenericPipe) {
			if (((LogisticsTileGenericPipe) tile).isPipeBlock()) {
				return true;
			}
		}

		return false; // super.isSideSolid removed
	}

	// removePipe moved to LogisticsTileGenericPipe#preRemoveSideEffects: 1.21.5 replaced onRemove
	// with affectNeighborsAfterRemoval, which runs after the block entity has been detached and so
	// could no longer reach the pipe.

	// @Override removed — dropBlockAsItemWithChance removed in 1.20.1
	public void dropBlockAsItemWithChance_DEAD(Level level, final BlockPos pos, BlockState state, float chance, int fortune) {

		if (level.isClientSide()) {
			return;
		}

		// quantityDropped removed in 1.20.1; drop once if chance passes
		if (level.getRandom().nextFloat() > chance) {
			return;
		}

		CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.getPipe(level, pos);

		if (pipe == null) {
			pipe = LogisticsBlockGenericPipe.pipeRemoved.get(new DoubleCoordinates(pos));
		}

		if (pipe == null) return;

		if (pipe.item != null && (pipe.canBeDestroyed() || pipe.destroyByPlayer())) {
			for (ItemStack stack : pipe.dropContents()) {
				Block.popResource(level, pos, stack);
			}
			Block.popResource(level, pos, new ItemStack(pipe.item, 1));
		} else if (pipe.item != null) {
			LogisticsBlockGenericPipe.cacheTileToPreventRemoval(pipe);
		}
	}

    public ItemStack getPickedResult(BlockState state, HitResult target, LevelReader levelReader, BlockPos pos, Player player) {
		ItemStack pick = getCloneItemStack(levelReader, pos, state, false, player);
		if (!pick.isEmpty()) {
			return pick;
		}
		Level level = (Level) levelReader; // safe — client-only code, LevelReader is always a Level here
		InternalRayTraceResult rayTraceResult = doRayTrace(level, pos, player);

		if (rayTraceResult != null && rayTraceResult.boundingBox != null) {
			if (rayTraceResult.hitPart == Part.PIPE) {
				final CoreUnroutedPipe pipe = Objects.requireNonNull(LogisticsBlockGenericPipe.getPipe(level, pos));
				return new ItemStack(pipe.item);
			}
		}
		return ItemStack.EMPTY;
	}

	// Forge's pick-block hook (middle click). Without this the block falls back to its own
	// (nonexistent) item and the client logs "Picking on: [BLOCK] logisticspipes:pipe gave
	// null item"; the pipe's actual item lives on the pipe object, not the block.
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData,
        Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof LogisticsTileGenericPipe genericPipe
            && LogisticsBlockGenericPipe.isValid(genericPipe.pipe)
            && genericPipe.pipe.item != null) {
            return new ItemStack(genericPipe.pipe.item);
        }
        return super.getCloneItemStack(level, pos, state, includeData, player);
    }

    /* Wrappers ************************************************************ */
	@Override
	protected void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn,
		@Nullable Orientation orientation, boolean movedByPiston) {
		super.neighborChanged(state, worldIn, pos, blockIn, orientation, movedByPiston);

		CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.getPipe(worldIn, pos);

		if (LogisticsBlockGenericPipe.isValid(pipe)) {
			pipe.container.scheduleNeighborChange();
		}
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.getPipe(level, pos);

		if (LogisticsBlockGenericPipe.isValid(pipe)) {
			pipe.onBlockPlaced();
			pipe.onBlockPlacedBy(placer);
			if (pipe instanceof IRotationProvider) {
				((IRotationProvider) pipe).setFacing(placer.getDirection().getOpposite());
			}
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		InteractionResult superResult = super.useWithoutItem(state, level, pos, player, hitResult);
		if (superResult != InteractionResult.PASS) return superResult;

		ItemStack heldItem = player.getInventory().getItem(player.getInventory().getSelectedSlot());

		CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.getPipe(level, pos);

		if (LogisticsBlockGenericPipe.isValid(pipe)) {

			if (heldItem.isEmpty()) {
				// Fall through the end of the test
			} else if (heldItem.getItem() instanceof SignItem) { // Items.SIGN removed in 1.20.1 — sign items split per wood type
				// Sign will be placed anyway, so lets show the sign gui
				return InteractionResult.PASS;
			} else if (heldItem.getItem() instanceof ItemLogisticsPipe) {
				return InteractionResult.PASS;
			}
			return pipe.blockActivated(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
		}

		return InteractionResult.PASS;
	}

	@Override
	public boolean isSignalSource(BlockState state) { // canProvidePower renamed to isSignalSource in 1.20.1
		return true;
	}

	// TODO: addHitEffects rendering — migrate to NeoForge IBlockExtension.addHitEffects with ParticleEngine (deferred)
	// 
	// @Override
	// public boolean addHitEffects(BlockState state, Level world, HitResult target, ParticleEngine effectRenderer) { ... }

	// TODO: addDestroyEffects rendering — migrate to NeoForge IBlockExtension.addDestroyEffects with ParticleEngine (deferred)
	// 
	// @Override
	// public boolean addDestroyEffects(BlockState state, Level world, BlockPos pos, ParticleEngine effectRenderer) { ... }

private void checkForRenderChanges(BlockGetter worldIn, BlockPos blockPos) {
		BlockEntity tile = new DoubleCoordinates(blockPos).getTileEntity(worldIn);
		if (!(tile instanceof LogisticsTileGenericPipe)) return;
		((LogisticsTileGenericPipe) tile).renderState.checkForRenderUpdate(worldIn, blockPos);
	}

	// @Override removed — canRenderInLayer removed in 1.20.1
	public boolean canRenderInLayer_DEAD(BlockState state, Object /* BlockRenderLayer */ layer) {
		return true;
	}
}
