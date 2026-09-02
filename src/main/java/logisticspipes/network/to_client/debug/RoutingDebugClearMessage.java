package logisticspipes.network.to_client.debug;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.routing.debug.ClientViewController;

/**
 * Throw away what the debugger is showing and start over.
 */
public record RoutingDebugClearMessage() implements CustomPacketPayload {

    public static final Type<RoutingDebugClearMessage> TYPE = new Type<>(LPConstants.rl("routing_debug_clear"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RoutingDebugClearMessage> STREAM_CODEC =
            StreamCodec.unit(new RoutingDebugClearMessage());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RoutingDebugClearMessage message, IPayloadContext context) {
        ClientViewController.instance().clear();
    }
}
