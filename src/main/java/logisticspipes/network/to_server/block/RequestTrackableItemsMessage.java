package logisticspipes.network.to_server.block;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.blocks.stats.LogisticsStatisticsTileEntity;
import logisticspipes.network.TargetLookup;
import logisticspipes.network.to_client.block.TrackableItemsMessage;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * The "add tracking" screen is opening and wants the list of items it can offer.
 *
 * <p>What the network holds is only known to the server, so the client asks and the server answers
 * with {@link TrackableItemsMessage}.
 */
public record RequestTrackableItemsMessage(BlockPos pos) implements CustomPacketPayload {

    public static final Type<RequestTrackableItemsMessage> TYPE =
            new Type<>(LPConstants.rl("request_trackable_items"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestTrackableItemsMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestTrackableItemsMessage::pos,
                    RequestTrackableItemsMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestTrackableItemsMessage message, IPayloadContext context) {
        final LogisticsStatisticsTileEntity be = TargetLookup.blockEntityAt(
                context.player(), message.pos, LogisticsStatisticsTileEntity.class);
        if (be == null || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        final CoreRoutedPipe pipe = be.getConnectedPipe();
        if (pipe == null) {
            return;
        }
        final var routers = pipe.getRouter().getIRoutersByCost();
        final Map<ItemIdentifier, Integer> available =
                SimpleServiceLocator.logisticsManager.getAvailableItems(routers);
        final LinkedList<ItemIdentifier> craftable =
                SimpleServiceLocator.logisticsManager.getCraftableItems(routers);

        // Sorted and deduplicated: an item that is both stocked and craftable is offered once,
        // showing what there is rather than a placeholder count.
        final TreeSet<ItemIdentifierStack> items = new TreeSet<>();
        available.forEach((item, amount) -> items.add(item.makeStack(amount)));
        craftable.stream().filter(item -> !available.containsKey(item))
                .forEach(item -> items.add(item.makeStack(1)));

        PacketDistributor.sendToPlayer(player, new TrackableItemsMessage(List.copyOf(items)));
    }
}
