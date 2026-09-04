package logisticspipes.network.to_client.block;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.client.gui.screen.StatisticsScreen;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * Everything the network can supply or craft, for the statistics block's "add tracking" screen.
 */
public record TrackableItemsMessage(List<ItemIdentifierStack> items) implements CustomPacketPayload {

    public static final Type<TrackableItemsMessage> TYPE = new Type<>(LPConstants.rl("trackable_items"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrackableItemsMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ItemIdentifierStack.STREAM_CODEC.apply(ByteBufCodecs.list()), TrackableItemsMessage::items,
                    TrackableItemsMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TrackableItemsMessage message, IPayloadContext context) {
        if (Minecraft.getInstance().screen instanceof StatisticsScreen gui) {
            gui.handleTrackableItems(message.items);
        }
    }
}
