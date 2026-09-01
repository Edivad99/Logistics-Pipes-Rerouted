package logisticspipes.network.to_client;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.ModuleTarget;
import logisticspipes.renderer.GuiOverlay;

/**
 * The neighbouring inventory's screen is open: highlight its slots so the player can pick one.
 *
 * <p>Everything the overlay needs to send the answer back travels here, because by the time the
 * player clicks, the screen on top belongs to that inventory and no longer knows about the pipe.
 *
 * @param inventoryPos the block whose screen the player is now looking at
 * @param slot         the index in the module's slot assignment pattern being filled in
 */
public record SlotFinderActivateMessage(ModuleTarget target, BlockPos inventoryPos, int slot)
        implements CustomPacketPayload {

    public static final Type<SlotFinderActivateMessage> TYPE =
            new Type<>(LPConstants.rl("slot_finder_activate"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SlotFinderActivateMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, SlotFinderActivateMessage::target,
                    BlockPos.STREAM_CODEC, SlotFinderActivateMessage::inventoryPos,
                    ByteBufCodecs.VAR_INT, SlotFinderActivateMessage::slot,
                    SlotFinderActivateMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SlotFinderActivateMessage message, IPayloadContext context) {
        GuiOverlay.getInstance().activate(message.target, message.inventoryPos, message.slot);
    }
}
