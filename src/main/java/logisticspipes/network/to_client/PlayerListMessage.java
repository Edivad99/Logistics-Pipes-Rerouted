package logisticspipes.network.to_client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.PlayerListReciver;

/**
 * The names of everyone online, in reply to the screen that asked for them.
 */
public record PlayerListMessage(List<String> playerNames) implements CustomPacketPayload {

    public static final Type<PlayerListMessage> TYPE = new Type<>(LPConstants.rl("player_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerListMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), PlayerListMessage::playerNames,
                    PlayerListMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlayerListMessage message, IPayloadContext context) {
        if (Minecraft.getInstance().screen instanceof PlayerListReciver receiver) {
            receiver.receivePlayerList(message.playerNames);
        }
    }
}
