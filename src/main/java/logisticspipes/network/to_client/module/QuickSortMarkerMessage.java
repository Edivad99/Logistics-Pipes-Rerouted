package logisticspipes.network.to_client.module;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.utils.QuickSortChestMarkerStorage;

/**
 * At least one quicksort module is aimed at the container the player just opened, so the client
 * can mark it.
 */
public record QuickSortMarkerMessage() implements CustomPacketPayload {

    public static final Type<QuickSortMarkerMessage> TYPE = new Type<>(LPConstants.rl("quick_sort_marker"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuickSortMarkerMessage> STREAM_CODEC =
            StreamCodec.unit(new QuickSortMarkerMessage());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuickSortMarkerMessage message, IPayloadContext context) {
        QuickSortChestMarkerStorage.getInstance().enable();
    }
}
