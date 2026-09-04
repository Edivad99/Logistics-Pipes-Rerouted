package logisticspipes.network.to_server.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.IBlockWatchingHandler;
import logisticspipes.network.TargetLookup;

/**
 * The client's HUD subscribes to a block that is not a pipe, or lets go of it.
 */
public record BlockHudWatchMessage(BlockPos pos, boolean watching) implements CustomPacketPayload {

    public static final Type<BlockHudWatchMessage> TYPE = new Type<>(LPConstants.rl("block_hud_watch"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockHudWatchMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, BlockHudWatchMessage::pos,
                    ByteBufCodecs.BOOL, BlockHudWatchMessage::watching,
                    BlockHudWatchMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BlockHudWatchMessage message, IPayloadContext context) {
        final IBlockWatchingHandler be = TargetLookup.blockEntityAt(
                context.player(), message.pos, IBlockWatchingHandler.class);
        if (be == null) {
            return;
        }
        if (message.watching) {
            be.playerStartWatching(context.player());
        } else {
            be.playerStopWatching(context.player());
        }
    }
}
