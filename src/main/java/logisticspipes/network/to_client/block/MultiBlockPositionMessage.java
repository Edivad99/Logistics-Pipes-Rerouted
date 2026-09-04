package logisticspipes.network.to_client.block;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.CoreMultiBlockPipe.SubBlockTypeForShare;
import logisticspipes.pipes.basic.LogisticsTileGenericSubMultiBlock;

/**
 * Which pipes a shared multi block belongs to, and what it stands in for.
 *
 * <p>A multi block pipe occupies several blocks; the extra ones are placeholders that have to be
 * told which real pipes they answer for.
 */
public record MultiBlockPositionMessage(
        BlockPos pos,
        Set<BlockPos> mainPipes,
        List<SubBlockTypeForShare> subTypes
) implements CustomPacketPayload {

    public static final Type<MultiBlockPositionMessage> TYPE = new Type<>(LPConstants.rl("multi_block_position"));

    public static final StreamCodec<FriendlyByteBuf, MultiBlockPositionMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, MultiBlockPositionMessage::pos,
                    ByteBufCodecs.collection(size -> new HashSet<>(), BlockPos.STREAM_CODEC),
                    MultiBlockPositionMessage::mainPipes,
                    NeoForgeStreamCodecs.<FriendlyByteBuf, SubBlockTypeForShare>enumCodec(SubBlockTypeForShare.class)
                            .apply(ByteBufCodecs.list()),
                    MultiBlockPositionMessage::subTypes,
                    MultiBlockPositionMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MultiBlockPositionMessage message, IPayloadContext context) {
        final LogisticsTileGenericSubMultiBlock be = TargetLookup.blockEntityAt(
                context.player(), message.pos, LogisticsTileGenericSubMultiBlock.class);
        if (be != null) {
            message.applyTo(be);
        }
    }

    public void applyTo(LogisticsTileGenericSubMultiBlock be) {
        be.setPosition(mainPipes, subTypes);
    }
}
