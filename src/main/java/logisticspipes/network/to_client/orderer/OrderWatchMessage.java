package logisticspipes.network.to_client.orderer;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.IRequestWatcher;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.request.resources.IResource;
import logisticspipes.routing.order.LinkedLogisticsOrderList;

/**
 * One watched request and the tree of orders it turned into, for the request table's monitor.
 *
 * <p>Sent every other tick while a player has the monitor open, and again to each player as they
 * open it. {@link OrdererWatchRemoveMessage} takes one back off the list when it finishes.
 *
 * @param watcherId the request's id, which is what the remove message names
 * @param resource  empty for a request made from a list rather than of one thing, which the
 *                  monitor shows as "List"
 */
public record OrderWatchMessage(BlockPos pos, int watcherId, Optional<IResource> resource,
        LinkedLogisticsOrderList orders) implements CustomPacketPayload {

    public static final Type<OrderWatchMessage> TYPE = new Type<>(LPConstants.rl("order_watch"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OrderWatchMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, OrderWatchMessage::pos,
                    ByteBufCodecs.VAR_INT, OrderWatchMessage::watcherId,
                    ByteBufCodecs.optional(IResource.STREAM_CODEC), OrderWatchMessage::resource,
                    LinkedLogisticsOrderList.STREAM_CODEC, OrderWatchMessage::orders,
                    OrderWatchMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OrderWatchMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be != null && be.pipe instanceof IRequestWatcher watcher) {
            watcher.handleClientSideListInfo(message.watcherId, message.resource.orElse(null), message.orders);
        }
    }
}
