package logisticspipes.network.to_server.orderer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.network.to_client.block.DiskContentMessage;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * The player ejected the disk from a request pipe.
 *
 * <p>Only the Mk2 pipe has a disk with nowhere to put it back: the request table's disk sits in a
 * real slot the player can just take it out of.
 */
public record DropDiskMessage(BlockPos pos) implements CustomPacketPayload {

    public static final Type<DropDiskMessage> TYPE = new Type<>(LPConstants.rl("drop_disk"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DropDiskMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, DropDiskMessage::pos,
                    DropDiskMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DropDiskMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be == null || !(be.pipe instanceof PipeItemsRequestLogisticsMk2 requestPipe)
                || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        requestPipe.dropDisk();
        // What the pipe holds now, which is nothing. The old packet sent the disk it had just
        // dropped, leaving the screen showing a disk the pipe no longer had.
        PacketDistributor.sendToPlayer(player, new DiskContentMessage(message.pos, requestPipe.getDisk()));
    }
}
