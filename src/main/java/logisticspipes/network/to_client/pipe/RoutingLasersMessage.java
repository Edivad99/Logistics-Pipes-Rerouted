package logisticspipes.network.to_client.pipe;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.renderer.LogisticsHUDRenderer;
import logisticspipes.routing.LaserData;

/**
 * The beam segments to draw for the pipe the player asked about.
 */
public record RoutingLasersMessage(List<LaserData> lasers) implements CustomPacketPayload {

    public static final Type<RoutingLasersMessage> TYPE = new Type<>(LPConstants.rl("routing_lasers"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RoutingLasersMessage> STREAM_CODEC =
            StreamCodec.composite(
                    LaserData.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    RoutingLasersMessage::lasers,
                    RoutingLasersMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RoutingLasersMessage message, IPayloadContext context) {
        LogisticsHUDRenderer.instance().setLasers(message.lasers);
    }
}
