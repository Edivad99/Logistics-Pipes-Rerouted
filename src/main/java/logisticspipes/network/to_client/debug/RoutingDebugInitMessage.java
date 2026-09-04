package logisticspipes.network.to_client.debug;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.routing.debug.ClientViewController;

/**
 * The debugger is starting: open its window.
 */
public record RoutingDebugInitMessage() implements CustomPacketPayload {

    public static final Type<RoutingDebugInitMessage> TYPE = new Type<>(LPConstants.rl("routing_debug_init"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RoutingDebugInitMessage> STREAM_CODEC =
            StreamCodec.unit(new RoutingDebugInitMessage());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RoutingDebugInitMessage message, IPayloadContext context) {
        ClientViewController.instance().init();
    }
}
