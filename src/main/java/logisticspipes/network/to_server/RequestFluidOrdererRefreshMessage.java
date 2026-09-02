package logisticspipes.network.to_server;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.RemotePipeTarget;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.request.RequestHandler;

/**
 * The fluid request GUI wants the list of what it can ask for.
 *
 * <p>The fluid orderer has no display options, which is the only thing that separates it from
 * {@link RequestOrdererRefreshMessage}.
 */
public record RequestFluidOrdererRefreshMessage(RemotePipeTarget target) implements CustomPacketPayload {

    public static final Type<RequestFluidOrdererRefreshMessage> TYPE =
            new Type<>(LPConstants.rl("request_fluid_orderer_refresh"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestFluidOrdererRefreshMessage> STREAM_CODEC =
            StreamCodec.composite(
                    RemotePipeTarget.STREAM_CODEC, RequestFluidOrdererRefreshMessage::target,
                    RequestFluidOrdererRefreshMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestFluidOrdererRefreshMessage message, IPayloadContext context) {
        final CoreRoutedPipe pipe = message.target.resolve();
        if (pipe != null) {
            RequestHandler.refreshFluid(context.player(), pipe);
        }
    }
}
