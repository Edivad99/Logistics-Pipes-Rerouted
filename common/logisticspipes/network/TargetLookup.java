package logisticspipes.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.network.PacketDistributor;

import org.jspecify.annotations.Nullable;

import logisticspipes.utils.gui.DummyContainer;

/**
 * Finding what a message is addressed to, without letting it decide what the server loads.
 *
 * <p>{@code Level.getBlockEntity} goes through {@code getChunkAt}, which loads -- or generates --
 * the chunk when it is absent. Coordinates in a message come from whoever sent it, so calling it
 * straight would let a client pull arbitrary chunks into memory, in any dimension it can name.
 * {@link Level#isLoaded} answers the same question without that side effect, and a message aimed
 * at something nobody has loaded is a message with nothing to do.
 */
public final class TargetLookup {

    private TargetLookup() {
    }

    /** The block entity of the given type at {@code pos}, or null if it is absent or not loaded. */
    public static <T> @Nullable T blockEntityAt(Level level, BlockPos pos, Class<T> type) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        final BlockEntity be = level.getBlockEntity(pos);
        return type.isInstance(be) ? type.cast(be) : null;
    }

    /** The same, in the receiving player's own level -- which is where all but the orderer look. */
    public static <T> @Nullable T blockEntityAt(Player player, BlockPos pos, Class<T> type) {
        return blockEntityAt(player.level(), pos, type);
    }

    /**
     * Sends a payload to everyone tracking the chunk a block entity sits in.
     *
     * <p>The chunk position comes from the block position rather than from asking the level for
     * the chunk: {@code getChunkAt} is a blocking full-status load, and this runs whenever a pipe
     * notifies its watchers -- during shutdown that can leave a chunk's future permanently
     * incomplete and hang the save.
     */
    public static void sendToChunkWatchers(@Nullable BlockEntity be, CustomPacketPayload payload) {
        if (be == null || !(be.getLevel() instanceof ServerLevel level)) {
            return;
        }
        PacketDistributor.sendToPlayersTrackingChunk(level, ChunkPos.containing(be.getBlockPos()), payload);
    }

    /**
     * The slot of the given type at {@code index} in the player's open container, or null.
     *
     * <p>The index is only meaningful against the container the player has open right now, so a
     * message that arrives after they closed it -- or with an index off the end -- resolves to
     * nothing rather than reaching into whatever menu happens to be open instead.
     */
    public static <T extends Slot> @Nullable T slotIn(Player player, int index, Class<T> type) {
        final AbstractContainerMenu menu = player.containerMenu;
        if (!(menu instanceof DummyContainer) || index < 0 || index >= menu.slots.size()) {
            return null;
        }
        final Slot slot = menu.getSlot(index);
        return type.isInstance(slot) ? type.cast(slot) : null;
    }
}
