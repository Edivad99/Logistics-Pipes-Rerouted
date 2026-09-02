package logisticspipes.network.to_client;

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

/**
 * An order the request GUI is watching has finished, so it can stop showing it.
 *
 * @param watcherId the order's id, or -1 to drop every order the GUI is showing
 */
public record OrdererWatchRemoveMessage(BlockPos pos, int watcherId) implements CustomPacketPayload {

    public static final Type<OrdererWatchRemoveMessage> TYPE =
            new Type<>(LPConstants.rl("orderer_watch_remove"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OrdererWatchRemoveMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, OrdererWatchRemoveMessage::pos,
                    ByteBufCodecs.VAR_INT, OrdererWatchRemoveMessage::watcherId,
                    OrdererWatchRemoveMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OrdererWatchRemoveMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be != null && be.pipe instanceof IRequestWatcher watcher) {
            watcher.handleClientSideRemove(message.watcherId);
        }
    }
}
