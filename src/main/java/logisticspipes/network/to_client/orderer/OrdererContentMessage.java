package logisticspipes.network.to_client.orderer;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.IAvailableItemsReceiver;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * Everything the network can currently supply or craft, for the orderer screen that asked.
 *
 * <p>Carries no position: it answers a refresh the open screen requested, so it goes to whichever
 * screen is still open.
 */
public record OrdererContentMessage(List<ItemIdentifierStack> available) implements CustomPacketPayload {

    public static final Type<OrdererContentMessage> TYPE = new Type<>(LPConstants.rl("orderer_content"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OrdererContentMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ItemIdentifierStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    OrdererContentMessage::available,
                    OrdererContentMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OrdererContentMessage message, IPayloadContext context) {
        if (Minecraft.getInstance().screen instanceof IAvailableItemsReceiver receiver) {
            receiver.setAvailableItems(message.available);
        }
    }
}
