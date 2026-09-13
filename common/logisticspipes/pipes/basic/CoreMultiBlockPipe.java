package logisticspipes.pipes.basic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.ITubeOrientation;
import logisticspipes.transport.LPTravelingItem;
import logisticspipes.transport.PipeMultiBlockTransportLogistics;
import logisticspipes.utils.PositionRotation;
import logisticspipes.utils.tuples.Pair;

public abstract class CoreMultiBlockPipe extends CoreUnroutedPipe {

	public enum SubBlockTypeForShare {
		NON_SHARE,
		S_CURVE_A,
		S_CURVE_B,
		CURVE_OUT_A,
		CURVE_INNER_A,
		CURVE_OUT_B,
		CURVE_INNER_B,
		GAIN_A,
		GAIN_B
	}

	private static final List<Pair<SubBlockTypeForShare, SubBlockTypeForShare>> allowedCombinations = new ArrayList<>();

	static {
		allowedCombinations.add(new Pair<>(SubBlockTypeForShare.S_CURVE_A, SubBlockTypeForShare.S_CURVE_B));
		allowedCombinations.add(new Pair<>(SubBlockTypeForShare.S_CURVE_A, SubBlockTypeForShare.S_CURVE_A));
		allowedCombinations.add(new Pair<>(SubBlockTypeForShare.CURVE_OUT_A, SubBlockTypeForShare.CURVE_INNER_A));
		allowedCombinations.add(new Pair<>(SubBlockTypeForShare.CURVE_OUT_B, SubBlockTypeForShare.CURVE_INNER_B));
		allowedCombinations.add(new Pair<>(SubBlockTypeForShare.CURVE_OUT_A, SubBlockTypeForShare.S_CURVE_A));
		allowedCombinations.add(new Pair<>(SubBlockTypeForShare.CURVE_OUT_B, SubBlockTypeForShare.S_CURVE_A));
		allowedCombinations.add(new Pair<>(SubBlockTypeForShare.GAIN_A, SubBlockTypeForShare.GAIN_B));
		allowedCombinations.add(new Pair<>(SubBlockTypeForShare.GAIN_A, SubBlockTypeForShare.GAIN_A));
	}

	public static boolean canShare(List<SubBlockTypeForShare> list, SubBlockTypeForShare toAdd) {
		if (toAdd == SubBlockTypeForShare.NON_SHARE) return false;
		if (toAdd == null) return false;
		if (list.size() > 1) return false;
		if (list.isEmpty()) return true;
		SubBlockTypeForShare contained = list.get(0);
		if (contained == SubBlockTypeForShare.NON_SHARE) return false;
		for (Pair<SubBlockTypeForShare, SubBlockTypeForShare> allowed : allowedCombinations) {
			if (allowed.getValue1() == contained) {
				if (allowed.getValue2() == toAdd) {
					return true;
				}
			}
			if (allowed.getValue2() == contained) {
				if (allowed.getValue1() == toAdd) {
					return true;
				}
			}
		}
		return false;
	}

	public CoreMultiBlockPipe(PipeMultiBlockTransportLogistics transport, Item item) {
		super(transport, item);
	}

	@Override
	public boolean isMultiBlock() {
		return true;
	}

	/**
	 * One block of a multiblock pipe: where it sits relative to the main block, and what it is
	 * willing to share that spot with.
	 */
	public record SubBlock(BlockPos offset, SubBlockTypeForShare type) {

		public SubBlock rotated(PositionRotation rotation) {
			return new SubBlock(rotation.apply(offset), type);
		}

		/** Where this block goes when the pipe is placed at {@code origin}. */
		public BlockPos at(BlockPos origin) {
			return origin.offset(offset);
		}
	}

	/**
	 * North Orientated
	 *
	 * @return Relative Positions
	 */
	public abstract List<SubBlock> getSubBlocks();

	public abstract List<SubBlock> getRotatedSubBlocks();

	public abstract void addCollisionBoxesToList(List<AABB> arraylist, @Nullable AABB axisalignedbb);

	public abstract AABB getCompleteBox();

	@Nullable
	public abstract ITubeOrientation getTubeOrientation(Player player, int xPos, int zPos);

	public abstract float getPipeLength();

	public double getDistanceWeight() {
		return 1.0 / 8.0;
	}

	public float getYawDiff(LPTravelingItem item) {
		return (float) (getItemRenderYaw(getPipeLength(), item) - getItemRenderYaw(0.0F, item));
	}

	public abstract @Nullable Direction getExitForInput(Direction comingFrom);

	@Nullable
	public abstract BlockEntity getConnectedEndTile(Direction output);

	@Override
	public abstract boolean actAsNormalPipe();

	@Override
	public boolean canPipeConnect(BlockEntity tile, Direction side) {
		if (tile instanceof LogisticsTileGenericSubMultiBlock) {
			return true;
		}
		if (tile instanceof LogisticsTileGenericPipe) {
			if (((LogisticsTileGenericPipe) tile).pipe.isMultiBlock()) {
				return true;
			}
		}
		return super.canPipeConnect(tile, side);
	}

}
