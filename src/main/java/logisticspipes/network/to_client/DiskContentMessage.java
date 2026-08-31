package logisticspipes.network.to_client;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * The disk a request pipe currently holds, for the GUI that is about to show it.
 */
public record DiskContentMessage(BlockPos pos, ItemStack disk) implements CustomPacketPayload {

    public static final Type<DiskContentMessage> TYPE = new Type<>(LPConstants.rl("disk_content"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiskContentMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, DiskContentMessage::pos,
                    ItemStack.OPTIONAL_STREAM_CODEC, DiskContentMessage::disk,
                    DiskContentMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DiskContentMessage message, IPayloadContext context) {
        final BlockEntity be = context.player().level().getBlockEntity(message.pos);
        if (!(be instanceof LogisticsTileGenericPipe container)) {
            return;
        }
        if (container.pipe instanceof PipeItemsRequestLogisticsMk2 requestPipe) {
            requestPipe.setDisk(message.disk);
        } else if (container.pipe instanceof PipeBlockRequestTable requestTable) {
            requestTable.diskInv.setItem(0, message.disk);
        }
    }
}
