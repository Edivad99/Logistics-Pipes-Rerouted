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
import logisticspipes.pipes.PipeItemsRequestLogistics;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.world.item.ItemDisk;

/**
 * A disk screen is opening and wants to see what is on the disk.
 *
 * <p>The disk lives in the pipe, and only the server has it; the client keeps a copy purely to
 * draw and edit, and sends the result back when the player saves.
 */
public record RequestDiskContentMessage(BlockPos pos) implements CustomPacketPayload {

    public static final Type<RequestDiskContentMessage> TYPE =
            new Type<>(LPConstants.rl("request_disk_content"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestDiskContentMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestDiskContentMessage::pos,
                    RequestDiskContentMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestDiskContentMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be == null || !(be.pipe instanceof PipeItemsRequestLogistics requester)
                || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        PacketDistributor.sendToPlayer(player,
                new DiskContentMessage(message.pos, ItemDisk.withData(requester.getDisk())));
    }
}
