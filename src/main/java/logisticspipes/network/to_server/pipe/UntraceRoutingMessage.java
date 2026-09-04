package logisticspipes.network.to_server.pipe;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.routing.debug.DebugController;

/**
 * Stop tracing one entry of the routing table in the debug HUD.
 */
public record UntraceRoutingMessage(int index) implements CustomPacketPayload {

    public static final Type<UntraceRoutingMessage> TYPE =
            new Type<>(LPConstants.rl("untrace_routing"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UntraceRoutingMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, UntraceRoutingMessage::index,
                    UntraceRoutingMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UntraceRoutingMessage message, IPayloadContext context) {
        DebugController.instance(context.player()).untrace(message.index);
    }
}
