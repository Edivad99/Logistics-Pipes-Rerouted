package logisticspipes.network.to_server.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.world.item.LPItems;

/**
 * A new name for the disk sitting in a request pipe.
 */
public record SetDiskNameMessage(BlockPos pos, String name) implements CustomPacketPayload {

    public static final Type<SetDiskNameMessage> TYPE = new Type<>(LPConstants.rl("set_disk_name"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetDiskNameMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetDiskNameMessage::pos,
                    ByteBufCodecs.STRING_UTF8, SetDiskNameMessage::name,
                    SetDiskNameMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetDiskNameMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe container =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (container == null || !(container.pipe instanceof PipeItemsRequestLogisticsMk2 requestPipe)) {
            return;
        }
        final ItemStack disk = requestPipe.getDisk();
        if (disk.isEmpty() || !disk.getItem().equals(LPItems.DISK.get())) {
            return;
        }
        disk.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, customData -> {
            final var tag = customData.copyTag();
            tag.putString("name", message.name);
            return CustomData.of(tag);
        });
    }
}
