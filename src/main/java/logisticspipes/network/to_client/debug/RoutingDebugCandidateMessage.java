package logisticspipes.network.to_client.debug;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.routing.debug.ClientViewController;
import logisticspipes.routing.debug.RouteDebugInfo;

/** A pipe the debugger has just added to the set it still has to look at. */
public record RoutingDebugCandidateMessage(RouteDebugInfo route) implements CustomPacketPayload {

    public static final Type<RoutingDebugCandidateMessage> TYPE =
            new Type<>(LPConstants.rl("routing_debug_candidate"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RoutingDebugCandidateMessage> STREAM_CODEC =
            StreamCodec.composite(
                    RouteDebugInfo.STREAM_CODEC, RoutingDebugCandidateMessage::route,
                    RoutingDebugCandidateMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RoutingDebugCandidateMessage message, IPayloadContext context) {
        ClientViewController.instance().addCandidate(message.route);
    }
}
