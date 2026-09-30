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
import net.minecraft.world.phys.Vec3;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import logisticspipes.LPConstants;
import logisticspipes.client.model.tube.TubeCollision;
import logisticspipes.client.model.tube.TubeModels;
import logisticspipes.interfaces.ITubeOrientation;
import logisticspipes.interfaces.ITubeRenderOrientation;
import logisticspipes.pipes.basic.CoreMultiBlockPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericSubMultiBlock;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.transport.LPTravelingItem;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemClient;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemServer;
import logisticspipes.transport.PipeMultiBlockTransportLogistics;
import logisticspipes.utils.IPositionRotateble;
import logisticspipes.utils.PositionRotation;

public class HSTubeSpeedup extends CoreMultiBlockPipe {

	@Getter
	private SpeedupDirection orientation;

	public HSTubeSpeedup(Item item) {
		super(new PipeMultiBlockTransportLogistics() {

			@Override
			public boolean canPipeConnect(BlockEntity tile, Direction side) {
				if (side.getOpposite() == ((HSTubeSpeedup) getMultiPipe()).orientation.dir1) {
					return super.canPipeConnect_internal(tile, side);
				}
				return false;
			}

			@Override
			protected void handleTileReachedServer(LPTravelingItemServer arrivingItem, @Nullable BlockEntity tile, @Nullable Direction dir) {
				if (dir.getOpposite() == ((HSTubeSpeedup) getMultiPipe()).orientation.dir1) {
					arrivingItem.setSpeed(LPConstants.PIPE_NORMAL_SPEED * 20);
					handleTileReachedServer_internal(arrivingItem, tile, dir);
				} else {
					super.handleTileReachedServer(arrivingItem, tile, dir);
				}
			}

			@Override
			protected void handleTileReachedClient(LPTravelingItemClient arrivingItem, @Nullable BlockEntity tile, @Nullable Direction dir) {
				if (dir.getOpposite() == ((HSTubeSpeedup) getMultiPipe()).orientation.dir1) {
					if (SimpleServiceLocator.pipeInformationManager.isItemPipe(tile)) {
						arrivingItem.setSpeed(LPConstants.PIPE_NORMAL_SPEED * 20);
						passToNextPipe(arrivingItem, tile);
					}
				} else {
					super.handleTileReachedClient(arrivingItem, tile, dir);
				}
			}

		}, item);
	}

	@Override
	public void writeState(FriendlyByteBuf buffer) {
		buffer.writeEnum(orientation);
	}

	@Override
	public void readState(FriendlyByteBuf buffer) {
		orientation = buffer.readEnum(SpeedupDirection.class);
	}

	@Override
	public List<SubBlock> getSubBlocks() {
		return List.of(
				new SubBlock(new BlockPos(0, 0, -1), SubBlockTypeForShare.NON_SHARE),
				new SubBlock(new BlockPos(0, 0, -2), SubBlockTypeForShare.NON_SHARE),
				new SubBlock(new BlockPos(0, 0, -3), SubBlockTypeForShare.NON_SHARE));
	}

	@Override
	public List<SubBlock> getRotatedSubBlocks() {
		PositionRotation rotation = new PositionRotation();
		orientation.rotatePositions(rotation);
		return getSubBlocks().stream().map(block -> block.rotated(rotation)).toList();
	}

	@Override
	public void addCollisionBoxesToList(List<AABB> arraylist, @Nullable AABB axisalignedbb) {
		BlockPos pos = getPos();
		PositionRotation rotation = new PositionRotation();
		orientation.rotatePositions(rotation);
		Vec3 posMin = rotation.apply(new Vec3(LPConstants.PIPE_MIN_POS, LPConstants.PIPE_MIN_POS, LPConstants.PIPE_MIN_POS));
		Vec3 posMax = rotation.apply(new Vec3(LPConstants.PIPE_MAX_POS, LPConstants.PIPE_MAX_POS, -3));
		if (orientation == SpeedupDirection.EAST) {
			pos = pos.offset(1, 0, 0);
		} else if (orientation == SpeedupDirection.SOUTH) {
			pos = pos.offset(1, 0, 1);
		} else if (orientation == SpeedupDirection.WEST) {
			pos = pos.offset(0, 0, 1);
		}
		AABB box = new AABB(posMin.x + pos.getX(), posMin.y + pos.getY(), posMin.z + pos.getZ(),
				posMax.x + pos.getX(), posMax.y + pos.getY(), posMax.z + pos.getZ());
		if (box != null && (axisalignedbb == null || axisalignedbb.intersects(box))) {
			arraylist.add(box);
		}
	}

	@Override
	public AABB getCompleteBox() {
		return TubeCollision.completeBox(TubeModels.Kind.SPEEDUP, orientation);
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
		for (SpeedupDirection ori : SpeedupDirection.values()) {
			if (ori.dir1.getOpposite().equals(dir)) {
				return ori;
			}
		}
		return null;
	}

	@Override
	public void serialize(ValueOutput output) {
		super.serialize(output);
		output.putString("orientation", orientation.name());
	}

	@Override
	public void deserialize(ValueInput input) {
		super.deserialize(input);
		orientation = SpeedupDirection.valueOf(input.getStringOr("orientation", ""));
	}

	@Override
	public float getPipeLength() {
		return 4;
	}

	@Override
	public @Nullable Direction getExitForInput(Direction comingFrom) {
		return comingFrom.getOpposite();
	}

	@Override
	@Nullable
	public BlockEntity getConnectedEndTile(Direction output) {
		if (orientation.dir1 == output) {
			PositionRotation rotation = new PositionRotation();
			orientation.rotatePositions(rotation);
			BlockPos offset = rotation.apply(new BlockPos(0, 0, -3));
			BlockEntity subTile = getLevel().getBlockEntity(getPos().offset(offset));
			if (subTile instanceof LogisticsTileGenericSubMultiBlock) {
				return ((LogisticsTileGenericSubMultiBlock) subTile).getTile(output);
			}
		} else if (orientation.dir1.getOpposite() == output) {
			return getContainer().getTile(output);
		}
		return null;
	}

	@Override
	public boolean actAsNormalPipe() {
		return true;
	}

	@Override
	public int getTextureIndex() {
		return 0;
	}

	@Override
	public boolean hasSpecialPipeEndAt(Direction dir) {
		return orientation != null && dir == orientation.dir1;
	}

	@Override
	public @Nullable Vec3 getItemRenderPos(float fPos, LPTravelingItem travelItem) {
		Vec3 pos = new Vec3(0.5D, 0.5D, 0.5D);
		float pPos = fPos;
		if (travelItem.input.getOpposite() == orientation.dir1) {
			pos = pos.relative(orientation.dir1, 3);
			pPos = this.getPipeLength() - fPos;
		}
		if (pPos < 0.5) {
			if (travelItem.input == null) {
				return null;
			}
			if (!getContainer().visualState.pipeConnectionMatrix.isConnected(travelItem.input.getOpposite())) {
				return null;
			}
			pos = pos.relative(travelItem.input.getOpposite(), 0.5 - fPos);
		} else {
			if (travelItem.output == null) {
				return null;
			}
			pos = pos.relative(travelItem.output, fPos - 0.5);
		}
		return pos;
	}

	@Override
	public boolean canPipeConnect(BlockEntity tile, Direction side) {
		if (tile instanceof LogisticsTileGenericSubMultiBlock) {
			if (this.getOrientation().getDir1() != side) {
				return false;
			}
		}
		return super.canPipeConnect(tile, side);
	}

	@Override
	public boolean isHSTube() {
		return true;
	}

	@AllArgsConstructor
	public enum SpeedupDirection implements ITubeRenderOrientation, ITubeOrientation {
		//@formatter:off
		NORTH(Direction.NORTH),
		SOUTH(Direction.SOUTH),
		EAST(Direction.EAST),
		WEST(Direction.WEST);
		//@formatter:on
		@Getter
		Direction dir1;

		@Override
		public void rotatePositions(IPositionRotateble set) {
			if (this == SOUTH) {
				set.rotateLeft();
				set.rotateLeft();
			} else if (this == EAST) {
				set.rotateRight();
			} else if (this == WEST) {
				set.rotateLeft();
			}
		}

		@Override
		public ITubeRenderOrientation getRenderOrientation() {
			return this;
		}

		@Override
		public BlockPos getOffset() {
			return BlockPos.ZERO;
		}

		@Override
		public void setOnPipe(CoreMultiBlockPipe pipe) {
			((HSTubeSpeedup) pipe).orientation = this;
		}
	}
}
