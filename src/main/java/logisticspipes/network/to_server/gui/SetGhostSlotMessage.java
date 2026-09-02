package logisticspipes.network.to_server.gui;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.utils.gui.DummySlot;
import logisticspipes.utils.gui.FluidSlot;

/**
 * An item dropped onto a filter slot, which shows a stack without holding one.
 *
 * <p>Only ghost slots accept this: a real slot would be a way to conjure items.
 */
public record SetGhostSlotMessage(int slotId, ItemStack stack) implements CustomPacketPayload {

    public static final Type<SetGhostSlotMessage> TYPE = new Type<>(LPConstants.rl("set_ghost_slot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetGhostSlotMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SetGhostSlotMessage::slotId,
                    ItemStack.OPTIONAL_STREAM_CODEC, SetGhostSlotMessage::stack,
                    SetGhostSlotMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetGhostSlotMessage message, IPayloadContext context) {
        final AbstractContainerMenu menu = context.player().containerMenu;
        if (message.slotId < 0 || message.slotId >= menu.slots.size()) {
            return;
        }
        final Slot slot = menu.getSlot(message.slotId);
        if (slot instanceof DummySlot || slot instanceof FluidSlot) {
            slot.set(message.stack);
        }
    }
}
