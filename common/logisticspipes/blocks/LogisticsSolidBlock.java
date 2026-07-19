package logisticspipes.blocks;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import logisticspipes.blocks.crafting.LogisticsCraftingTableTileEntity;
import logisticspipes.blocks.powertile.LogisticsIC2PowerProviderTileEntity;
import logisticspipes.blocks.powertile.LogisticsPowerJunctionTileEntity;
import logisticspipes.blocks.powertile.LogisticsRFPowerProviderTileEntity;
import logisticspipes.blocks.stats.LogisticsStatisticsTileEntity;
import logisticspipes.interfaces.IGuiTileEntity;
import logisticspipes.interfaces.IRotationProvider;
import logisticspipes.interfaces.ITickable;
import logisticspipes.proxy.MainProxy;
import logisticspipes.world.level.block.entity.LogisticsProgramCompilerBlockEntity;

import logisticspipes.world.level.block.entity.LogisticsSolidBlockEntity;

import lombok.Getter;
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

public class LogisticsSolidBlock extends Block implements EntityBlock {

	public static final IntegerProperty rotationProperty = IntegerProperty.create("rotation", 0, 3);
	public static final BooleanProperty active = BooleanProperty.create("active");
	public static final Map<Direction, BooleanProperty> connectionPropertys = Arrays.stream(Direction.values())
			.collect(Collectors.toMap(key -> key, key -> BooleanProperty.create("connection_" + key.ordinal())));

	@Getter
	private final Type type;

	public enum Type {
		LOGISTICS_POWER_JUNCTION(1, LogisticsPowerJunctionTileEntity::new),
		LOGISTICS_SECURITY_STATION(2, LogisticsSecurityTileEntity::new),
		LOGISTICS_AUTOCRAFTING_TABLE(3, LogisticsCraftingTableTileEntity::new),
		LOGISTICS_FUZZYCRAFTING_TABLE(4, LogisticsCraftingTableTileEntity::new),
		LOGISTICS_STATISTICS_TABLE(5, LogisticsStatisticsTileEntity::new),

		// Power Provider
		LOGISTICS_RF_POWERPROVIDER(10, LogisticsRFPowerProviderTileEntity::new),
		LOGISTICS_IC2_POWERPROVIDER(11, LogisticsIC2PowerProviderTileEntity::new),
		LOGISTICS_BC_POWERPROVIDER(12),

		LOGISTICS_PROGRAM_COMPILER(14, LogisticsProgramCompilerBlockEntity::new),

		LOGISTICS_BLOCK_FRAME(15, LogisticsFrameTileEntity::new);

		/** Numeric meta id kept for {@link logisticspipes.datafixer.DataFixerSolidBlockItems} legacy save migration. */
		@Getter
		int meta;

		@Getter
		boolean hasActiveTexture;

		@Nullable
		private final BiFunction<BlockPos, BlockState, BlockEntity> teConstructor;

		Type(int meta) {
			this(meta, null, false);
		}

		Type(int meta, @Nullable BiFunction<BlockPos, BlockState, BlockEntity> teConstructor) {
			this(meta, teConstructor, false);
		}

		Type(int meta, @Nullable BiFunction<BlockPos, BlockState, BlockEntity> teConstructor, boolean hasActiveTexture) {
			this.meta = meta;
			this.teConstructor = teConstructor;
			this.hasActiveTexture = hasActiveTexture;
		}

		public boolean hasTE() {
			return teConstructor != null;
		}

		public BlockEntity createTE(BlockPos pos, BlockState state) {
			if (!hasTE()) throw new UnsupportedOperationException("This block type has no tile entity!");
			assert teConstructor != null;
			return teConstructor.apply(pos, state);
		}
	}

	public LogisticsSolidBlock(Type type) {
		// noOcclusion() is required so the BER receives a non-zero packedLight value.
		// Without it Minecraft treats the block as fully opaque, stores sky-light = 0
		// at its own position, and the BER renders pitch-black regardless of ambient light.
		super(Properties.of().strength(6.0F).requiresCorrectToolForDrops().noOcclusion());
		this.type = type;
	}

	@Override
	public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
		super.neighborChanged(state, world, pos, block, fromPos, isMoving);
		BlockEntity tile = world.getBlockEntity(pos);
		if (tile instanceof LogisticsSolidBlockEntity) {
			((LogisticsSolidBlockEntity) tile).notifyOfBlockChange();
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!player.isCrouching()) {
			BlockEntity tile = level.getBlockEntity(pos);
			if (tile instanceof IGuiTileEntity) {
				if (MainProxy.isServer(player.level())) {
					logisticspipes.network.abstractguis.CoordinatesGuiProvider gp = ((IGuiTileEntity) tile).getGuiProvider();
					gp.setTilePos(tile).open(player);
				}
				return InteractionResult.sidedSuccess(level.isClientSide);
			}
		}
		return InteractionResult.PASS;
	}

	@Override
	public void setPlacedBy(@Nonnull Level world, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable LivingEntity placer, @Nonnull ItemStack stack) {
		super.setPlacedBy(world, pos, state, placer, stack);
		BlockEntity tile = world.getBlockEntity(pos);
		if (tile instanceof LogisticsCraftingTableTileEntity) {
			((LogisticsCraftingTableTileEntity) tile).placedBy(placer);
		}
		if (placer != null && tile instanceof IRotationProvider) {
			((IRotationProvider) tile).setFacing(placer.getDirection().getOpposite());
		}
	}

	@Override
	public void onRemove(@Nonnull BlockState state, @Nonnull Level worldIn, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
		if (state.getBlock() != newState.getBlock()) {
			BlockEntity tile = worldIn.getBlockEntity(pos);
			if (tile instanceof LogisticsSolidBlockEntity) {
				((LogisticsSolidBlockEntity) tile).onBlockBreak();
			}
		}
		super.onRemove(state, worldIn, pos, newState, isMoving);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
		if (!type.hasTE()) return null;
		return type.createTE(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
		// Tick all ITickable solid block entities
		return (lvl, pos, st, be) -> {
			if (be instanceof ITickable) ((ITickable) be).update();
		};
	}

	@Override
	@Nonnull
	public RenderShape getRenderShape(@Nonnull BlockState state) {
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

	// TODO: getActualState (dynamic state per neighbor) removed in 1.20.1.
	// Reimplement as a ticker that calls setChanged() + requestModelDataUpdate(),
	// or encode connection state in blockstate updates via neighborChanged().

}
