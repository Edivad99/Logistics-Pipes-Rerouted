package logisticspipes.network.to_server.orderer;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.request.RequestHandler;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * A whole recipe's worth of ingredients requested at once from a request table.
 */
public record SubmitRequestListMessage(BlockPos pos, List<ItemIdentifierStack> request)
        implements CustomPacketPayload {

    public static final Type<SubmitRequestListMessage> TYPE =
            new Type<>(LPConstants.rl("submit_request_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SubmitRequestListMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SubmitRequestListMessage::pos,
                    ItemIdentifierStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    SubmitRequestListMessage::request,
                    SubmitRequestListMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SubmitRequestListMessage message, IPayloadContext context) {
        final PipeBlockRequestTable table =
                TargetLookup.blockEntityOrPipeAt(context.player(), message.pos, PipeBlockRequestTable.class);
        if (table != null) {
            RequestHandler.requestList(context.player(), message.request, table);
        }
    }
}
