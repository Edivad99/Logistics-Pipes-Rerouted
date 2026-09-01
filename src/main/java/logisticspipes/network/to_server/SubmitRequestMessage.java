package logisticspipes.network.to_server;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.RemotePipeTarget;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.request.RequestHandler;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * A request typed into an orderer GUI.
 */
public record SubmitRequestMessage(RemotePipeTarget target, ItemIdentifierStack stack) implements CustomPacketPayload {

    public static final Type<SubmitRequestMessage> TYPE = new Type<>(LPConstants.rl("submit_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SubmitRequestMessage> STREAM_CODEC =
            StreamCodec.composite(
                    RemotePipeTarget.STREAM_CODEC, SubmitRequestMessage::target,
                    ItemIdentifierStack.STREAM_CODEC, SubmitRequestMessage::stack,
                    SubmitRequestMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SubmitRequestMessage message, IPayloadContext context) {
        final CoreRoutedPipe pipe = message.target.resolve();
        if (pipe != null) {
            RequestHandler.request(context.player(), message.stack, pipe);
        }
    }
}
