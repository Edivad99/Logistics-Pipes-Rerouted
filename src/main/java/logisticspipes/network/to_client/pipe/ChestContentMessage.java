package logisticspipes.network.to_client.pipe;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.IChestContentReceiver;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * What sits in the inventory a pipe is attached to, for the players watching its HUD.
 */
public record ChestContentMessage(BlockPos pos, List<ItemIdentifierStack> contents)
        implements CustomPacketPayload {

    public static final Type<ChestContentMessage> TYPE = new Type<>(LPConstants.rl("chest_content"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChestContentMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ChestContentMessage::pos,
                    ItemIdentifierStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    ChestContentMessage::contents,
                    ChestContentMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChestContentMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be != null && be.pipe instanceof IChestContentReceiver receiver) {
            receiver.setReceivedChestContent(message.contents);
        }
    }
}
