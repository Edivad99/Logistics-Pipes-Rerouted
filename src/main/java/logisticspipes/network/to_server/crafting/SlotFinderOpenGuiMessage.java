package logisticspipes.network.to_server.crafting;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.SlotFinder;

/**
 * The player pressed "Set" on one of the supplier module's pattern slots.
 *
 * <p>Picking a slot means opening the neighbouring inventory's own screen, which only the server
 * can do, so the work happens in {@link SlotFinder} and the answer comes back as
 * {@link logisticspipes.network.to_client.crafting.SlotFinderActivateMessage}.
 *
 * @param slot the index in the module's slot assignment pattern the player is filling in
 */
public record SlotFinderOpenGuiMessage(ModuleTarget target, int slot) implements CustomPacketPayload {

    public static final Type<SlotFinderOpenGuiMessage> TYPE =
            new Type<>(LPConstants.rl("slot_finder_open_gui"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SlotFinderOpenGuiMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, SlotFinderOpenGuiMessage::target,
                    ByteBufCodecs.VAR_INT, SlotFinderOpenGuiMessage::slot,
                    SlotFinderOpenGuiMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SlotFinderOpenGuiMessage message, IPayloadContext context) {
        SlotFinder.openNeighbourInventory(context.player(), message.target, message.slot);
    }
}
