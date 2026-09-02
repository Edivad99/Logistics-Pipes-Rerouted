package logisticspipes.network.to_server.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.blocks.stats.LogisticsStatisticsTileEntity;
import logisticspipes.network.TargetLookup;
import logisticspipes.utils.item.ItemIdentifier;

/**
 * The player started or stopped tracking an item on a statistics block.
 *
 * <p>Adding and removing are one message with a flag: same block, same item, same table, and the
 * two buttons sit next to each other.
 */
public record TrackItemMessage(BlockPos pos, ItemIdentifier item, boolean tracked)
        implements CustomPacketPayload {

    public static final Type<TrackItemMessage> TYPE = new Type<>(LPConstants.rl("track_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrackItemMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, TrackItemMessage::pos,
                    ItemIdentifier.STREAM_CODEC, TrackItemMessage::item,
                    ByteBufCodecs.BOOL, TrackItemMessage::tracked,
                    TrackItemMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TrackItemMessage message, IPayloadContext context) {
        final LogisticsStatisticsTileEntity be = TargetLookup.blockEntityAt(
                context.player(), message.pos, LogisticsStatisticsTileEntity.class);
        if (be != null) {
            be.setTracked(message.item, message.tracked);
        }
    }
}
