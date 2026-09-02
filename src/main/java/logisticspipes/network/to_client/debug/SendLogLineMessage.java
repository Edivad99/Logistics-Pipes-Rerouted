package logisticspipes.network.to_client.debug;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.pipes.basic.debug.LogWindow;

/**
 * One more line for a pipe's log window.
 *
 * <p>The window is a Swing frame the client opens outside the game; the id says which one, since a
 * player can follow several pipes at once.
 */
public record SendLogLineMessage(int windowId, String line) implements CustomPacketPayload {

    public static final Type<SendLogLineMessage> TYPE = new Type<>(LPConstants.rl("log_line"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SendLogLineMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SendLogLineMessage::windowId,
                    ByteBufCodecs.STRING_UTF8, SendLogLineMessage::line,
                    SendLogLineMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SendLogLineMessage message, IPayloadContext context) {
        LogWindow.getWindow(message.windowId).newLine(message.line);
    }
}
