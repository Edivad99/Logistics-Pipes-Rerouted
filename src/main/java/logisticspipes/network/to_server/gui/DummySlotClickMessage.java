package logisticspipes.network.to_server.gui;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.utils.gui.ColorSlot;
import logisticspipes.world.inventory.DummyMenu;
import logisticspipes.utils.gui.DummySlot;
import logisticspipes.utils.gui.FluidSlot;


/**
 * The player clicked one of a screen's ghost slots.
 *
 * <p>Ghost slots hold a filter rather than an item, so vanilla's own click handling would move the
 * stack instead of setting it; the screen sends what the slot should show and the server writes it.
 *
 * @param button which mouse button, since a right click sets the count rather than the item
 */
public record DummySlotClickMessage(int slotId, ItemStack stack, int button) implements CustomPacketPayload {

    public static final Type<DummySlotClickMessage> TYPE = new Type<>(LPConstants.rl("dummy_slot_click"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DummySlotClickMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, DummySlotClickMessage::slotId,
                    ItemStack.OPTIONAL_STREAM_CODEC, DummySlotClickMessage::stack,
                    ByteBufCodecs.VAR_INT, DummySlotClickMessage::button,
                    DummySlotClickMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DummySlotClickMessage message, IPayloadContext context) {
        if (!(context.player().containerMenu instanceof DummyMenu menu)) {
            return;
        }
        menu.applyGhostSlotEdit(message.slotId, message.stack, message.button, context.player());
    }
}
