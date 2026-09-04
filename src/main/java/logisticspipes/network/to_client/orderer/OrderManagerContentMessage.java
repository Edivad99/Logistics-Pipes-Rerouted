package logisticspipes.network.to_client.orderer;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.IOrderManagerContentReceiver;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * What a pipe's order manager still has outstanding, for the players watching its HUD.
 */
public record OrderManagerContentMessage(BlockPos pos, List<ItemIdentifierStack> orders)
        implements CustomPacketPayload {

    public static final Type<OrderManagerContentMessage> TYPE =
            new Type<>(LPConstants.rl("order_manager_content"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OrderManagerContentMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, OrderManagerContentMessage::pos,
                    ItemIdentifierStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    OrderManagerContentMessage::orders,
                    OrderManagerContentMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OrderManagerContentMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be != null && be.pipe instanceof IOrderManagerContentReceiver receiver) {
            receiver.setOrderManagerContent(message.orders);
        }
    }
}
