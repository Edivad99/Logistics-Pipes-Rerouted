package logisticspipes.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import org.jspecify.annotations.Nullable;

import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;

public final class BlockEntityBuffer {

    private @Nullable Block block = null;
    private @Nullable BlockEntity blockEntity;

    private final SafeTimeTracker tracker = new SafeTimeTracker(20, 5);
    private final Level level;
    private final int x, y, z;
    private final boolean loadUnloaded;

    public BlockEntityBuffer(Level level, int x, int y, int z, boolean loadUnloaded) {
        this.level = level;
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
        LevelChunk chunk = level.getChunkSource().getChunkNow(
            SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        if (chunk == null) {
            // Not resident: nothing can be observed about this neighbour right now. Leave the
            // cached values untouched rather than reporting "no block", which is what the
            // timer-based callers already expect between refreshes.
            return;
        }
        BlockState blockState = chunk.getBlockState(pos);
        if (blockEntity instanceof LogisticsGenericPipeBlockEntity pipeEntity && pipeEntity.pipe != null
            && pipeEntity.pipe.preventRemove() && blockState.isAir()) {
            return;
        }
        blockEntity = null;
        block = null;

        if (!loadUnloaded) {
            return;
        }

        block = blockState.getBlock();

        if (blockState.hasBlockEntity()) {
            blockEntity = chunk.getBlockEntity(pos);
        }
    }

    public void set(Block block, BlockEntity blockEntity) {
        this.block = block;
        this.blockEntity = blockEntity;
        tracker.markTime(level);
    }

    public @Nullable Block getBlock() {
        if (blockEntity != null && !blockEntity.isRemoved()) {
            return block;
        }

        if (tracker.markTimeIfDelay(level)) {
            refresh();

            if (blockEntity != null && !blockEntity.isRemoved()) {
                return block;
            }
        }

        return null;
    }

    public @Nullable BlockEntity getBlockEntity() {
        if (blockEntity != null && !blockEntity.isRemoved()) {
            return blockEntity;
        }

        if (tracker.markTimeIfDelay(level)) {
            refresh();

            if (blockEntity != null && !blockEntity.isRemoved()) {
                return blockEntity;
            }
        }

        return null;
    }

    public static BlockEntityBuffer[] makeBuffer(Level level, BlockPos pos, boolean loadUnloaded) {
        BlockEntityBuffer[] buffer = new BlockEntityBuffer[6];

        for (int i = 0; i < 6; i++) {
            Direction d = Direction.from3DDataValue(i);
            buffer[i] = new BlockEntityBuffer(level, pos.getX() + d.getStepX(), pos.getY() + d.getStepY(),
                pos.getZ() + d.getStepZ(), loadUnloaded);
        }

        return buffer;
    }
}
