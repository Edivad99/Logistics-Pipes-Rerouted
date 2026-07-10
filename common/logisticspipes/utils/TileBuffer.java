package logisticspipes.utils;

import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class TileBuffer {

	private Block block = null;
	private BlockEntity tile;

	private final SafeTimeTracker tracker = new SafeTimeTracker(20, 5);
	private final Level world;
	private final int x, y, z;
	private final boolean loadUnloaded;

	public TileBuffer(Level world, int x, int y, int z, boolean loadUnloaded) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.loadUnloaded = loadUnloaded;

		refresh();
	}

	public void refresh() {
		BlockPos pos = new BlockPos(x, y, z);
		BlockState blockState = world.getBlockState(pos);
		if (tile instanceof LogisticsTileGenericPipe && ((LogisticsTileGenericPipe) tile).pipe != null && ((LogisticsTileGenericPipe) tile).pipe.preventRemove()) {
			if (blockState.isAir()) {
				return;
			}
		}
		tile = null;
		block = null;

		if (!loadUnloaded) {
			return;
		}

		block = blockState.getBlock();

		if (blockState.hasBlockEntity()) {
			tile = world.getBlockEntity(pos);
		}
	}

	public void set(Block block, BlockEntity tile) {
		this.block = block;
		this.tile = tile;
		tracker.markTime(world);
	}

	public Block getBlock() {
		if (tile != null && !tile.isRemoved()) {
			return block;
		}

		if (tracker.markTimeIfDelay(world)) {
			refresh();

			if (tile != null && !tile.isRemoved()) {
				return block;
			}
		}

		return null;
	}

	public BlockEntity getTile() {
		if (tile != null && !tile.isRemoved()) {
			return tile;
		}

		if (tracker.markTimeIfDelay(world)) {
			refresh();

			if (tile != null && !tile.isRemoved()) {
				return tile;
			}
		}

		return null;
	}

	public static TileBuffer[] makeBuffer(Level world, BlockPos pos, boolean loadUnloaded) {
		TileBuffer[] buffer = new TileBuffer[6];

		for (int i = 0; i < 6; i++) {
			Direction d = Direction.from3DDataValue(i);
			buffer[i] = new TileBuffer(world, pos.getX() + d.getStepX(), pos.getY() + d.getStepY(), pos.getZ() + d.getStepZ(), loadUnloaded);
		}

		return buffer;
	}
}
