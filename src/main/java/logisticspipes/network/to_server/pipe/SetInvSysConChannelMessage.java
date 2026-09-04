package logisticspipes.network.to_server.pipe;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.PipeItemsInvSysConnector;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * The channel picked for an inventory system connector.
 *
 * <p>The channel travels as a {@link UUID}; it used to be its {@code toString}, parsed back on
 * arrival, so a malformed string threw inside the handler instead of failing to decode.
 */
public record SetInvSysConChannelMessage(BlockPos pos, UUID channel) implements CustomPacketPayload {

    public static final Type<SetInvSysConChannelMessage> TYPE =
            new Type<>(LPConstants.rl("set_inv_sys_con_channel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetInvSysConChannelMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetInvSysConChannelMessage::pos,
                    UUIDUtil.STREAM_CODEC, SetInvSysConChannelMessage::channel,
                    SetInvSysConChannelMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetInvSysConChannelMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe container =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (container != null && container.pipe instanceof PipeItemsInvSysConnector connector) {
            connector.setChannelFromClient(message.channel);
        }
    }
}
