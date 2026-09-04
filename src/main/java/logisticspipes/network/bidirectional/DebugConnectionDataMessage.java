package logisticspipes.network.bidirectional;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.commands.commands.debug.DebugGuiController;

/**
 * One message of the object inspector's own protocol, in either direction.
 *
 * <p>The bytes belong to the debug GUI library and mean nothing here; all this side knows is which
 * connection they belong to.
 */
public record DebugConnectionDataMessage(int connectionId, byte[] payload) implements CustomPacketPayload {

    public static final Type<DebugConnectionDataMessage> TYPE = new Type<>(LPConstants.rl("debug_connection_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DebugConnectionDataMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, DebugConnectionDataMessage::connectionId,
                    ByteBufCodecs.BYTE_ARRAY, DebugConnectionDataMessage::payload,
                    DebugConnectionDataMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DebugConnectionDataMessage message, IPayloadContext context) {
        DebugGuiController.instance().handleDataPacket(message.payload, message.connectionId, context.player());
    }
}
