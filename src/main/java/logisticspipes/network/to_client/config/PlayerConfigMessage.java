package logisticspipes.network.to_client.config;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;

import network.rs485.logisticspipes.config.ClientConfiguration;

/**
 * The settings the server has stored for this player, sent as they log in.
 *
 * <p>They are the player's own, not the world's: the server keeps them so that they follow the
 * player between clients.
 */
public record PlayerConfigMessage(int renderPipeDistance, int renderPipeContentDistance)
        implements CustomPacketPayload {

    public static final Type<PlayerConfigMessage> TYPE = new Type<>(LPConstants.rl("player_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerConfigMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, PlayerConfigMessage::renderPipeDistance,
                    ByteBufCodecs.VAR_INT, PlayerConfigMessage::renderPipeContentDistance,
                    PlayerConfigMessage::new);

    public static PlayerConfigMessage of(ClientConfiguration config) {
        return new PlayerConfigMessage(config.getRenderPipeDistance(), config.getRenderPipeContentDistance());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlayerConfigMessage message, IPayloadContext context) {
        final ClientConfiguration config = LogisticsPipes.getClientPlayerConfig();
        config.setRenderPipeDistance(message.renderPipeDistance);
        config.setRenderPipeContentDistance(message.renderPipeContentDistance);
    }
}
