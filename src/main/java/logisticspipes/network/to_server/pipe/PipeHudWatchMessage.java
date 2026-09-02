package logisticspipes.network.to_server.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.IWatchingHandler;
import logisticspipes.interfaces.IWatchingHandler.WatchMode;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * The client's HUD subscribes to a pipe, or lets go of it.
 *
 * <p>The watching mode is not sent: the HUD is the only thing that sends this, so the mode
 * is always {@link WatchMode#HUD}. The GUI's own mode is set from the server side.
 */
public record PipeHudWatchMessage(BlockPos pos, boolean watching) implements CustomPacketPayload {

    public static final Type<PipeHudWatchMessage> TYPE = new Type<>(LPConstants.rl("pipe_hud_watch"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PipeHudWatchMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PipeHudWatchMessage::pos,
                    ByteBufCodecs.BOOL, PipeHudWatchMessage::watching,
                    PipeHudWatchMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PipeHudWatchMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be = TargetLookup.blockEntityAt(
                context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be == null || !(be.pipe instanceof IWatchingHandler handler)) {
            return;
        }
        if (message.watching) {
            handler.playerStartWatching(context.player(), WatchMode.HUD);
        } else {
            handler.playerStopWatching(context.player(), WatchMode.HUD);
        }
    }
}
