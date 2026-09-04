package logisticspipes.network.to_client.debug;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.routing.debug.ClientViewController;
import logisticspipes.routing.debug.RouteDebugInfo;

/** The pipe the debugger is currently working outwards from. */
public record RoutingDebugSourceMessage(RouteDebugInfo route) implements CustomPacketPayload {

    public static final Type<RoutingDebugSourceMessage> TYPE =
            new Type<>(LPConstants.rl("routing_debug_source"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RoutingDebugSourceMessage> STREAM_CODEC =
            StreamCodec.composite(
                    RouteDebugInfo.STREAM_CODEC, RoutingDebugSourceMessage::route,
                    RoutingDebugSourceMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RoutingDebugSourceMessage message, IPayloadContext context) {
        ClientViewController.instance().setSource(message.route);
    }
}
