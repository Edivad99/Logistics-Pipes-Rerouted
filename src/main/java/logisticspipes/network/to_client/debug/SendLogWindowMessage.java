package logisticspipes.network.to_client.debug;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.pipes.basic.debug.LogWindow;

/**
 * The title of a pipe's log window.
 *
 * <p>The window is a Swing frame the client opens outside the game; the id says which one, since a
 * player can follow several pipes at once.
 */
public record SendLogWindowMessage(int windowId, String title) implements CustomPacketPayload {

    public static final Type<SendLogWindowMessage> TYPE = new Type<>(LPConstants.rl("log_window"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SendLogWindowMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SendLogWindowMessage::windowId,
                    ByteBufCodecs.STRING_UTF8, SendLogWindowMessage::title,
                    SendLogWindowMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SendLogWindowMessage message, IPayloadContext context) {
        LogWindow.getWindow(message.windowId).setTitle(message.title);
    }
}
