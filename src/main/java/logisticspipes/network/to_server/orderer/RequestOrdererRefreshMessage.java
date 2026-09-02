package logisticspipes.network.to_server.orderer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.RemotePipeTarget;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.request.RequestHandler;
import logisticspipes.request.RequestHandler.DisplayOptions;

/**
 * The request GUI wants the list of what it can ask for.
 *
 * <p>Addressed by {@link RemotePipeTarget} because the remote orderer opens this GUI on a pipe in
 * another dimension.
 *
 * <p>The display option travels as itself. It used to be an int the receiver read as {@code n % 10}
 * and mapped back through a {@code switch}, so an unrecognised value silently meant "Both".
 */
public record RequestOrdererRefreshMessage(RemotePipeTarget target, DisplayOptions options)
        implements CustomPacketPayload {

    public static final Type<RequestOrdererRefreshMessage> TYPE =
            new Type<>(LPConstants.rl("request_orderer_refresh"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestOrdererRefreshMessage> STREAM_CODEC =
            StreamCodec.composite(
                    RemotePipeTarget.STREAM_CODEC, RequestOrdererRefreshMessage::target,
                    NeoForgeStreamCodecs.enumCodec(DisplayOptions.class), RequestOrdererRefreshMessage::options,
                    RequestOrdererRefreshMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestOrdererRefreshMessage message, IPayloadContext context) {
        final CoreRoutedPipe pipe = message.target.resolve();
        if (pipe != null) {
            RequestHandler.refresh(context.player(), pipe, message.options);
        }
    }
}
