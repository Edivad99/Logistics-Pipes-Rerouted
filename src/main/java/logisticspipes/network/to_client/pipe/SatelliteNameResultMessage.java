package logisticspipes.network.to_client.pipe;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.client.gui.screen.SatellitePipeScreen;
import logisticspipes.pipes.SatelliteNamingResult;

/**
 * How renaming a satellite went, for the screen that asked for it.
 */
public record SatelliteNameResultMessage(SatelliteNamingResult result, String name) implements CustomPacketPayload {

    public static final Type<SatelliteNameResultMessage> TYPE = new Type<>(LPConstants.rl("satellite_name_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SatelliteNameResultMessage> STREAM_CODEC =
        StreamCodec.composite(
            NeoForgeStreamCodecs.enumCodec(
                SatelliteNamingResult.class),
            SatelliteNameResultMessage::result,
            ByteBufCodecs.STRING_UTF8, SatelliteNameResultMessage::name,
            SatelliteNameResultMessage::new);

    public static void handle(SatelliteNameResultMessage message, IPayloadContext context) {
        Client.handle(message, context);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static final class Client {

        static void handle(SatelliteNameResultMessage message, IPayloadContext context) {
            if (Minecraft.getInstance().gui.screen() instanceof SatellitePipeScreen gui) {
                gui.handleResponse(message.result, message.name);
            }
        }
    }
}
