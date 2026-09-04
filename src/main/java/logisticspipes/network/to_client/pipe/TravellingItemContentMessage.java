package logisticspipes.network.to_client.pipe;

import java.lang.ref.WeakReference;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.transport.LPTravelingItem;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemClient;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;

/**
 * What an item travelling through a pipe actually is.
 *
 * <p>Sent once per item id: the position updates that follow carry only the id, so the client
 * learns the stack here and remembers it.
 */
public record TravellingItemContentMessage(int travelId, ItemIdentifierStack item) implements CustomPacketPayload {

    /** How long a newly announced item is kept alive before the weak reference may go. */
    private static final int KEEP_TICKS = 10;

    public static final Type<TravellingItemContentMessage> TYPE =
            new Type<>(LPConstants.rl("travelling_item_content"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TravellingItemContentMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TravellingItemContentMessage::travelId,
                    ItemIdentifierStack.STREAM_CODEC, TravellingItemContentMessage::item,
                    TravellingItemContentMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TravellingItemContentMessage message, IPayloadContext context) {
        final WeakReference<LPTravelingItemClient> ref = LPTravelingItem.clientList.get(message.travelId);
        final LPTravelingItemClient known = ref != null ? ref.get() : null;
        if (known != null) {
            known.setItem(message.item);
            return;
        }
        final LPTravelingItemClient item = new LPTravelingItemClient(message.travelId, message.item);
        LPTravelingItem.clientList.put(message.travelId, new WeakReference<>(item));
        synchronized (LPTravelingItem.forceKeep) {
            LPTravelingItem.forceKeep.add(new Pair<>(KEEP_TICKS, item));
        }
    }
}
