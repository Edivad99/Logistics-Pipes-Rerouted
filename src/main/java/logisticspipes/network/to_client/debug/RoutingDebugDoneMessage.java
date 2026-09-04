package logisticspipes.network.to_client.debug;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.routing.debug.ClientViewController;

/**
 * The debugger has finished: close its window.
 */
public record RoutingDebugDoneMessage() implements CustomPacketPayload {

    public static final Type<RoutingDebugDoneMessage> TYPE = new Type<>(LPConstants.rl("routing_debug_done"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RoutingDebugDoneMessage> STREAM_CODEC =
            StreamCodec.unit(new RoutingDebugDoneMessage());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RoutingDebugDoneMessage message, IPayloadContext context) {
        ClientViewController.instance().done();
    }
}
