package logisticspipes.network.to_client.pipe;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.routing.order.IOrderInfoProvider;

/**
 * Everything a pipe's order manager is holding, for the pipe controller's order list.
 *
 * <p>The whole list travels every time it changes: it is short, only a player with the controller
 * open ever receives it, and sending the state outright is what makes a dropped update harmless.
 */
public record PipeOrdersMessage(BlockPos pos, List<IOrderInfoProvider> orders) implements CustomPacketPayload {

    public static final Type<PipeOrdersMessage> TYPE = new Type<>(LPConstants.rl("pipe_orders"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PipeOrdersMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PipeOrdersMessage::pos,
                    IOrderInfoProvider.STREAM_CODEC.apply(ByteBufCodecs.list()), PipeOrdersMessage::orders,
                    PipeOrdersMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PipeOrdersMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be != null && be.pipe instanceof CoreRoutedPipe pipe) {
            pipe.getClientSideOrderManager().clear();
            pipe.getClientSideOrderManager().addAll(message.orders);
        }
    }
}
