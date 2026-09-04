package logisticspipes.network.to_client.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.IRotationProvider;
import logisticspipes.network.TargetLookup;

/**
 * Which way a block faces.
 *
 * <p>Rotation is not part of the block state, so it does not come down with the chunk; the client
 * asks for it with {@link logisticspipes.network.to_server.block.RequestBlockRotationMessage} when the
 * block entity loads.
 */
public record BlockRotationMessage(BlockPos pos, int rotation) implements CustomPacketPayload {

    public static final Type<BlockRotationMessage> TYPE = new Type<>(LPConstants.rl("block_rotation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockRotationMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, BlockRotationMessage::pos,
                    ByteBufCodecs.VAR_INT, BlockRotationMessage::rotation,
                    BlockRotationMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BlockRotationMessage message, IPayloadContext context) {
        final IRotationProvider target =
                TargetLookup.blockEntityOrPipeAt(context.player(), message.pos, IRotationProvider.class);
        if (target == null) {
            return;
        }
        target.setRotation(message.rotation);
        context.player().level().updateNeighborsAt(
                message.pos, context.player().level().getBlockState(message.pos).getBlock());
    }
}
