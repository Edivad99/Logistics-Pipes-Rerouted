package logisticspipes.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import net.neoforged.neoforge.server.ServerLifecycleHooks;

import org.jspecify.annotations.Nullable;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * A pipe addressed from a GUI that may not be standing next to it.
 *
 * <p>This is the one place a dimension belongs in a message. {@code ModernPacket} writes one for
 * every packet, but the receiving side almost always uses the player's own level; the remote
 * orderer is the exception -- it opens a request GUI on a pipe in another dimension entirely, so
 * the message has to say which.
 */
public record RemotePipeTarget(Identifier dimension, BlockPos pos) {

    public static final StreamCodec<RegistryFriendlyByteBuf, RemotePipeTarget> STREAM_CODEC =
            StreamCodec.composite(
                    Identifier.STREAM_CODEC, RemotePipeTarget::dimension,
                    BlockPos.STREAM_CODEC, RemotePipeTarget::pos,
                    RemotePipeTarget::new);

    /**
     * Finds the pipe, or null when it is not loaded -- or when the dimension itself is gone.
     *
     * <p>Server-side only: it walks the server's levels, which is the whole point of carrying a
     * dimension rather than reading the receiving player's.
     */
    public @Nullable CoreRoutedPipe resolve() {
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        final ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (level == null) {
            return null;
        }
        final LogisticsTileGenericPipe container =
                TargetLookup.blockEntityAt(level, pos, LogisticsTileGenericPipe.class);
        return container != null && container.pipe instanceof CoreRoutedPipe pipe ? pipe : null;
    }
}
