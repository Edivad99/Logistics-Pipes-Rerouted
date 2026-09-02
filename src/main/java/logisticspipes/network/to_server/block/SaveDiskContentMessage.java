package logisticspipes.network.to_server.block;

import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.network.TargetLookup;
import logisticspipes.LPConstants;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.world.item.LPItems;

/**
 * The player edited a disk in a request pipe's GUI: write its contents back.
 *
 * <p>Only the custom data is taken, and only onto a disk that is already in the pipe -- the
 * message can rewrite what a disk remembers, never what item sits in the slot.
 */
public record SaveDiskContentMessage(BlockPos pos, ItemStack disk) implements CustomPacketPayload {

    public static final Type<SaveDiskContentMessage> TYPE =
            new Type<>(LPConstants.rl("save_disk_content"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveDiskContentMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SaveDiskContentMessage::pos,
                    ItemStack.OPTIONAL_STREAM_CODEC, SaveDiskContentMessage::disk,
                    SaveDiskContentMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SaveDiskContentMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe container =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (container == null) {
            return;
        }
        if (container.pipe instanceof PipeItemsRequestLogisticsMk2 requestPipe) {
            copyDiskData(message.disk, requestPipe.getDisk());
        } else if (container.pipe instanceof PipeBlockRequestTable requestTable) {
            copyDiskData(message.disk, requestTable.diskInv.getItem(0));
        }
    }

    /** Copies what the sent disk remembers onto the one in the pipe, if both really are disks. */
    private static void copyDiskData(ItemStack sent, ItemStack inPipe) {
        if (inPipe.isEmpty() || !inPipe.getItem().equals(LPItems.DISK.get())) {
            return;
        }
        if (sent.isEmpty() || !sent.getItem().equals(LPItems.DISK.get()) || !sent.has(DataComponents.CUSTOM_DATA)) {
            return;
        }
        inPipe.set(DataComponents.CUSTOM_DATA,
                CustomData.of(Objects.requireNonNull(sent.get(DataComponents.CUSTOM_DATA)).copyTag()));
    }
}
