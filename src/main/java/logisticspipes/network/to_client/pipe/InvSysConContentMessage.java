package logisticspipes.network.to_client.pipe;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.client.gui.screen.InvSysConnectorScreen;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * What an inventory system connector is still expecting to arrive.
 *
 * <p>Goes to the screen that asked rather than to the pipe: the list is a snapshot for display,
 * and the pipe keeps its own.
 */
public record InvSysConContentMessage(List<ItemIdentifierStack> expected) implements CustomPacketPayload {

    public static final Type<InvSysConContentMessage> TYPE =
            new Type<>(LPConstants.rl("inv_sys_con_content"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InvSysConContentMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ItemIdentifierStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    InvSysConContentMessage::expected,
                    InvSysConContentMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(InvSysConContentMessage message, IPayloadContext context) {
        if (Minecraft.getInstance().screen instanceof InvSysConnectorScreen gui) {
            gui.handleContentAnswer(message.expected);
        }
    }
}
