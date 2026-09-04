package logisticspipes.network.to_client.pipe;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.ISendQueueContentRecieiver;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * What a pipe still has queued to send, for the players watching its HUD.
 */
public record SendQueueContentMessage(BlockPos pos, List<ItemIdentifierStack> queued)
        implements CustomPacketPayload {

    public static final Type<SendQueueContentMessage> TYPE = new Type<>(LPConstants.rl("send_queue_content"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SendQueueContentMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SendQueueContentMessage::pos,
                    ItemIdentifierStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    SendQueueContentMessage::queued,
                    SendQueueContentMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SendQueueContentMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be != null && be.pipe instanceof ISendQueueContentRecieiver receiver) {
            receiver.handleSendQueueItemIdentifierList(message.queued);
        }
    }
}
