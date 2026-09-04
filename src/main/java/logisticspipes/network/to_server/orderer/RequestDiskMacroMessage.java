package logisticspipes.network.to_server.orderer;

import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.PipeItemsRequestLogistics;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.request.RequestHandler;
import logisticspipes.world.item.LPItems;

/**
 * The player picked a saved macro off a request disk.
 *
 * <p>What travels is which macro, not what is in it: the disk is in the pipe, and the server would
 * have to check the contents anyway.
 *
 * @param macro the macro's index in the disk's list
 */
public record RequestDiskMacroMessage(BlockPos pos, int macro) implements CustomPacketPayload {

    public static final Type<RequestDiskMacroMessage> TYPE = new Type<>(LPConstants.rl("request_disk_macro"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestDiskMacroMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestDiskMacroMessage::pos,
                    ByteBufCodecs.VAR_INT, RequestDiskMacroMessage::macro,
                    RequestDiskMacroMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestDiskMacroMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (be == null || !(be.pipe instanceof PipeItemsRequestLogistics requester)) {
            return;
        }
        final ListTag macros = macroList(requester.getDisk());
        if (message.macro < 0 || message.macro >= macros.size()) {
            return;
        }
        RequestHandler.requestMacrolist(macros.getCompoundOrEmpty(message.macro), requester, context.player());
    }

    /** The macros saved on a disk, or an empty list when the stack is not a disk or holds none. */
    private static ListTag macroList(ItemStack disk) {
        if (disk.isEmpty() || !disk.getItem().equals(LPItems.DISK.get())
                || !disk.has(DataComponents.CUSTOM_DATA)) {
            return new ListTag();
        }
        final CompoundTag data = Objects.requireNonNull(disk.get(DataComponents.CUSTOM_DATA)).copyTag();
        return data.getListOrEmpty("macroList");
    }
}
