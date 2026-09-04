package logisticspipes.network.to_client.module;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.ModuleTarget;
import logisticspipes.utils.QuickSortChestMarkerStorage;

/**
 * The slot a quicksort module is currently working on, for the players watching it.
 *
 * <p>The only message so far that never looks up a module: the marker storage is keyed by the
 * position and the module's slot, both of which {@link ModuleTarget} already carries.
 */
public record QuickSortStateMessage(ModuleTarget target, int workingSlot) implements CustomPacketPayload {

    public static final Type<QuickSortStateMessage> TYPE =
            new Type<>(LPConstants.rl("quick_sort_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuickSortStateMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, QuickSortStateMessage::target,
                    ByteBufCodecs.VAR_INT, QuickSortStateMessage::workingSlot,
                    QuickSortStateMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuickSortStateMessage message, IPayloadContext context) {
        QuickSortChestMarkerStorage.getInstance().setSlots(
                message.target.pos().getX(),
                message.target.pos().getY(),
                message.target.pos().getZ(),
                message.target.positionInt(),
                message.workingSlot);
    }
}
