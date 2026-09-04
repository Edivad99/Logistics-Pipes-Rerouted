package logisticspipes.network.to_client.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.SatellitePipe;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * The name a satellite pipe answers to, for the clients that can see it.
 *
 * <p>Sent to a player opening the GUI, to the ones watching the HUD, and to everyone tracking the
 * chunk -- the name is drawn on the pipe itself.
 */
public record SatelliteNameMessage(BlockPos pos, String name) implements CustomPacketPayload {

    public static final Type<SatelliteNameMessage> TYPE = new Type<>(LPConstants.rl("satellite_name"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SatelliteNameMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SatelliteNameMessage::pos,
                    ByteBufCodecs.STRING_UTF8, SatelliteNameMessage::name,
                    SatelliteNameMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SatelliteNameMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe container =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (container != null && container.pipe instanceof SatellitePipe satellite) {
            satellite.setSatellitePipeName(message.name);
        }
    }
}
