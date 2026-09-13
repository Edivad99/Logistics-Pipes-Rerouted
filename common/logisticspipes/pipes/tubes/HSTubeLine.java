package logisticspipes.pipes.tubes;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import logisticspipes.client.model.tube.TubeCollision;
import logisticspipes.client.model.tube.TubeModels;
import logisticspipes.interfaces.ITubeOrientation;
import logisticspipes.interfaces.ITubeRenderOrientation;
import logisticspipes.pipes.basic.CoreMultiBlockPipe;
import logisticspipes.transport.PipeMultiBlockTransportLogistics;
import logisticspipes.utils.IPositionRotateble;
import logisticspipes.utils.PositionRotation;

public class HSTubeLine extends CoreMultiBlockPipe {

	@Getter
	private TubeLineOrientation orientation;

	public HSTubeLine(Item item) {
		super(new PipeMultiBlockTransportLogistics(), item);
	}

	@Override
	public void writeState(FriendlyByteBuf buffer) {
		buffer.writeNullable(orientation, FriendlyByteBuf::writeEnum);
	}

	@Override
	public void readState(FriendlyByteBuf buffer) {
		final TubeLineOrientation read = buffer.readNullable(inner -> inner.readEnum(TubeLineOrientation.class));
		if (read != null) {
			orientation = read;
		}
	}

	@Override
	public List<SubBlock> getSubBlocks() {
		return List.of();
	}

	@Override
	public List<SubBlock> getRotatedSubBlocks() {
		PositionRotation rotation = new PositionRotation();
		orientation.rotatePositions(rotation);
		return getSubBlocks().stream().map(block -> block.rotated(rotation)).toList();
	}

	@Override
	public void serialize(ValueOutput output) {
		super.serialize(output);
		output.putString("orientation", orientation.name());
	}

	@Override
	public void deserialize(ValueInput input) {
		super.deserialize(input);
		orientation = TubeLineOrientation.valueOf(input.getStringOr("orientation", ""));
	}

	@Override
	public void addCollisionBoxesToList(List<AABB> arraylist, @Nullable AABB axisalignedbb) {
		BlockPos pos = getPos();
		AABB box = TubeCollision.completeBox(TubeModels.Kind.LINE, orientation)
				.move(pos.getX(), pos.getY(), pos.getZ());
		if (box != null && (axisalignedbb == null || axisalignedbb.intersects(box))) {
			arraylist.add(box);
		}
	}

	@Override
	public AABB getCompleteBox() {
		return TubeCollision.completeBox(TubeModels.Kind.LINE, orientation);
	}

	@Override
	@Nullable
	public ITubeOrientation getTubeOrientation(Player player, int xPos, int zPos) {
		double x = xPos + 0.5 - player.getX();
		double z = zPos + 0.5 - player.getZ();
		double w = Math.atan2(x, z);
		double halfPI = Math.PI / 2;
		double halfhalfPI = halfPI / 2;
		w -= halfhalfPI;
		if (w < 0) {
			w += 2 * Math.PI;
		}
		Direction dir = null;
		if (0 < w && w <= halfPI) {
			dir = Direction.WEST;
		} else if (halfPI < w && w <= 2 * halfPI) {
			dir = Direction.SOUTH;
		} else if (2 * halfPI < w && w <= 3 * halfPI) {
			dir = Direction.EAST;
		} else if (3 * halfPI < w && w <= 4 * halfPI) {
			dir = Direction.NORTH;
		}
		for (TubeLineOrientation ori : TubeLineOrientation.values()) {
			if (ori.dir.equals(dir)) {
				return ori;
			}
		}
		return null;
	}

	@Override
	public float getPipeLength() {
		return 1;
	}

	@Override
	public @Nullable Direction getExitForInput(Direction comingFrom) {
		return comingFrom.getOpposite();
	}

	@Override
	@Nullable
	public BlockEntity getConnectedEndTile(Direction output) {
		if (output == this.orientation.dir || output.getOpposite() == this.orientation.dir) {
			return getContainer().getTile(output);
		}
		return null;
	}

	@Override
	public int getIconIndex(@Nullable Direction direction) {
		return 0;
	}

	@Override
	public int getTextureIndex() {
		return 0;
	}

	@Override
	public boolean actAsNormalPipe() {
		return false;
	}

	@Override
	public boolean isHSTube() {
		return true;
	}

	public enum TubeLineOrientation implements ITubeOrientation {
		NORTH(TubeLineRenderOrientation.NORTH_SOUTH, new BlockPos(0, 0, 0), Direction.NORTH),
		SOUTH(TubeLineRenderOrientation.NORTH_SOUTH, new BlockPos(0, 0, 0), Direction.SOUTH),
		EAST(TubeLineRenderOrientation.EAST_WEST, new BlockPos(0, 0, 0), Direction.EAST),
		WEST(TubeLineRenderOrientation.EAST_WEST, new BlockPos(0, 0, 0), Direction.WEST);

		@Getter
		TubeLineRenderOrientation renderOrientation;
		@Getter
		BlockPos offset;
		@Getter
		Direction dir;

		TubeLineOrientation(TubeLineRenderOrientation render, BlockPos off, Direction dir) {
			renderOrientation = render;
			offset = off;
			this.dir = dir;
		}

		@Override
		public void rotatePositions(IPositionRotateble set) {
			renderOrientation.rotateOrientation(set);
		}

		@Override
		public void setOnPipe(CoreMultiBlockPipe pipe) {
			((HSTubeLine) pipe).orientation = this;
		}
	}

	public enum TubeLineRenderOrientation implements ITubeRenderOrientation {
		NORTH_SOUTH(Direction.NORTH),
		EAST_WEST(Direction.EAST);

		@Getter
		private Direction dir;

		TubeLineRenderOrientation(Direction dir) {
			this.dir = dir;
		}

		public void rotateOrientation(IPositionRotateble set) {
			if (this == EAST_WEST) {
				set.rotateLeft();
			}
		}
	}
}
