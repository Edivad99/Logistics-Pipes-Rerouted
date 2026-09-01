package logisticspipes.network.to_server;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.BlockPos;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.SlotFinder;

/**
 * The player clicked a slot in the neighbouring inventory's screen.
 *
 * <p>What travels is the slot's index in the <em>open menu</em>, which is the only handle the
 * client has. Turning it into an index into the inventory itself needs the block, so the server
 * does it in {@link SlotFinder}.
 *
 * @param inventoryPos  the block the player is picking a slot in
 * @param menuSlotIndex the clicked slot's index in the open container menu
 * @param slot          the index in the module's slot assignment pattern being filled in
 */
public record SlotFinderSlotMessage(ModuleTarget target, BlockPos inventoryPos, int menuSlotIndex, int slot)
        implements CustomPacketPayload {

    public static final Type<SlotFinderSlotMessage> TYPE = new Type<>(LPConstants.rl("slot_finder_slot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SlotFinderSlotMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, SlotFinderSlotMessage::target,
                    BlockPos.STREAM_CODEC, SlotFinderSlotMessage::inventoryPos,
                    ByteBufCodecs.VAR_INT, SlotFinderSlotMessage::menuSlotIndex,
                    ByteBufCodecs.VAR_INT, SlotFinderSlotMessage::slot,
                    SlotFinderSlotMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SlotFinderSlotMessage message, IPayloadContext context) {
        SlotFinder.assignSlot(context.player(), message.target, message.inventoryPos,
                message.menuSlotIndex, message.slot);
    }
}
