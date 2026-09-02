package logisticspipes.network.to_client.debug;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.commands.commands.debug.DebugGuiController;

/**
 * Opens the object inspector for something the player asked to watch.
 *
 * <p>The connection id has to match the one the server allocated: it is how every later
 * {@link logisticspipes.network.bidirectional.DebugConnectionDataMessage} finds its panel.
 */
public record OpenDebugPanelMessage(String name, int connectionId) implements CustomPacketPayload {

    public static final Type<OpenDebugPanelMessage> TYPE = new Type<>(LPConstants.rl("open_debug_panel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenDebugPanelMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, OpenDebugPanelMessage::name,
                    ByteBufCodecs.VAR_INT, OpenDebugPanelMessage::connectionId,
                    OpenDebugPanelMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenDebugPanelMessage message, IPayloadContext context) {
        DebugGuiController.instance().createNewDebugGui(message.name, message.connectionId);
    }
}
