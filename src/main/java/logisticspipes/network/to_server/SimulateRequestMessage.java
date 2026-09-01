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
public record SimulateRequestMessage(RemotePipeTarget target, ItemIdentifierStack stack) implements CustomPacketPayload {

    public static final Type<SimulateRequestMessage> TYPE = new Type<>(LPConstants.rl("simulate_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SimulateRequestMessage> STREAM_CODEC =
            StreamCodec.composite(
                    RemotePipeTarget.STREAM_CODEC, SimulateRequestMessage::target,
                    ItemIdentifierStack.STREAM_CODEC, SimulateRequestMessage::stack,
                    SimulateRequestMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SimulateRequestMessage message, IPayloadContext context) {
        final CoreRoutedPipe pipe = message.target.resolve();
        if (pipe != null) {
            RequestHandler.simulate(context.player(), message.stack, pipe);
        }
    }
}
