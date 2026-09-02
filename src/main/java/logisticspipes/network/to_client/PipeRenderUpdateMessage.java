package logisticspipes.network.to_client;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * A pipe's connections changed in a way the client cannot see coming, so it should look again.
 *
 * <p>Nothing but the position travels: the client works the new shape out for itself. The old
 * packet also carried an integer, which no sender ever set and the receiver never read.
 */
public record PipeRenderUpdateMessage(BlockPos pos) implements CustomPacketPayload {

    public static final Type<PipeRenderUpdateMessage> TYPE =
            new Type<>(LPConstants.rl("pipe_render_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PipeRenderUpdateMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PipeRenderUpdateMessage::pos,
                    PipeRenderUpdateMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PipeRenderUpdateMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be != null) {
            be.renderState.checkForRenderUpdate(context.player().level(), message.pos);
        }
    }
}
