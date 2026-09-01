package logisticspipes.network.to_client;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.network.ModuleTarget;

/**
 * The recipe laid out in a crafting module's dummy inventory, for the clients that can see it.
 *
 * <p>These are the slots of a recipe, so they travel as {@link ItemStack} -- the only message of
 * the inventory family that does. The other nine carry {@code ItemIdentifierStack}, which is a
 * type plus a count and cannot express an empty slot.
 */
public record CraftingDummyInventoryMessage(ModuleTarget target, List<ItemStack> slots)
        implements CustomPacketPayload {

    public static final Type<CraftingDummyInventoryMessage> TYPE =
            new Type<>(LPConstants.rl("crafting_dummy_inventory"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingDummyInventoryMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, CraftingDummyInventoryMessage::target,
                    ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
                    CraftingDummyInventoryMessage::slots,
                    CraftingDummyInventoryMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CraftingDummyInventoryMessage message, IPayloadContext context) {
        final ModuleCrafter module = message.target.resolve(context.player(), ModuleCrafter.class);
        if (module == null) {
            return;
        }
        for (int slot = 0; slot < message.slots.size() && slot < module.dummyInventory.getContainerSize(); slot++) {
            module.dummyInventory.setItem(slot, message.slots.get(slot));
        }
    }
}
