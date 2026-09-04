package logisticspipes.network.to_server.config;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.utils.PlayerIdentifier;

import network.rs485.logisticspipes.config.ClientConfiguration;

/**
 * The player changed their settings in the mod's own options screen.
 */
public record SetPlayerConfigMessage(int renderPipeDistance, int renderPipeContentDistance)
        implements CustomPacketPayload {

    public static final Type<SetPlayerConfigMessage> TYPE = new Type<>(LPConstants.rl("set_player_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetPlayerConfigMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SetPlayerConfigMessage::renderPipeDistance,
                    ByteBufCodecs.VAR_INT, SetPlayerConfigMessage::renderPipeContentDistance,
                    SetPlayerConfigMessage::new);

    public static SetPlayerConfigMessage of(ClientConfiguration config) {
        return new SetPlayerConfigMessage(config.getRenderPipeDistance(),
                config.getRenderPipeContentDistance());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetPlayerConfigMessage message, IPayloadContext context) {
        final ClientConfiguration config = new ClientConfiguration();
        config.setRenderPipeDistance(message.renderPipeDistance);
        config.setRenderPipeContentDistance(message.renderPipeContentDistance);
        LogisticsPipes.getServerConfigManager()
                .setClientConfiguration(PlayerIdentifier.get(context.player()), config);
    }
}
