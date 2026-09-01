package logisticspipes.network.to_server;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.routing.IRequestFluid;
import logisticspipes.network.RemotePipeTarget;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.request.RequestHandler;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * A fluid request typed into the fluid orderer GUI.
 */
public record SubmitFluidRequestMessage(RemotePipeTarget target, ItemIdentifierStack stack)
        implements CustomPacketPayload {

    public static final Type<SubmitFluidRequestMessage> TYPE =
            new Type<>(LPConstants.rl("submit_fluid_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SubmitFluidRequestMessage> STREAM_CODEC =
            StreamCodec.composite(
                    RemotePipeTarget.STREAM_CODEC, SubmitFluidRequestMessage::target,
                    ItemIdentifierStack.STREAM_CODEC, SubmitFluidRequestMessage::stack,
                    SubmitFluidRequestMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SubmitFluidRequestMessage message, IPayloadContext context) {
        final CoreRoutedPipe pipe = message.target.resolve();
        if (pipe instanceof IRequestFluid requestFluid) {
            RequestHandler.requestFluid(context.player(), message.stack, pipe, requestFluid);
        }
    }
}
