package logisticspipes.network.to_client.debug;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.routing.debug.ClientViewController;
import logisticspipes.routing.debug.RouteDebugInfo;

/** The whole candidate set as it stands, for the debug window's list. */
public record RoutingDebugCandidateListMessage(List<RouteDebugInfo> routes) implements CustomPacketPayload {

    public static final Type<RoutingDebugCandidateListMessage> TYPE =
            new Type<>(LPConstants.rl("routing_debug_candidate_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RoutingDebugCandidateListMessage> STREAM_CODEC =
            StreamCodec.composite(
                    RouteDebugInfo.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    RoutingDebugCandidateListMessage::routes,
                    RoutingDebugCandidateListMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RoutingDebugCandidateListMessage message, IPayloadContext context) {
        ClientViewController.instance().updateList(message.routes);
    }
}
