package logisticspipes.utils;

import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.LevelChunk;
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
		// Resolve through the resident chunk rather than through world.getBlockState() /
		// world.getBlockEntity(): those route via getChunkAt(), which *loads* the chunk at FULL
		// status when it is not in memory. A neighbour lookup must never do that. It is fatal
		// during chunk unload: clearAllBlockEntities -> pipe.setRemoved -> CoreRoutedPipe
		// .invalidate -> ServerRouter.destroy -> the adjacency rescan reaches this method and
		// re-requests the chunk being unloaded, registering a TicketType.UNKNOWN ticket from
		// inside ChunkMap.processUnloads — after that tick's purgeStaleTickets. The ticket can
		// then never expire, the chunk never becomes isReadyForSaving(), and scheduleUnload
		// busy-retries forever: the server hangs on "Saving world" at 100% CPU.
		LevelChunk chunk = world.getChunkSource().getChunkNow(
				SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
		if (chunk == null) {
			// Not resident: nothing can be observed about this neighbour right now. Leave the
			// cached values untouched rather than reporting "no block", which is what the
			// timer-based callers already expect between refreshes.
			return;
		}
		BlockState blockState = chunk.getBlockState(pos);
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
			tile = chunk.getBlockEntity(pos);
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
