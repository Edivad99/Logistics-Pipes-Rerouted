package logisticspipes.network.to_server.module;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.modules.ModuleItemSink;
import logisticspipes.network.ModuleTarget;
import logisticspipes.utils.item.ItemIdentifier;

/**
 * The player pressed "import" in the item sink's GUI.
 *
 * <p>What sits in the neighbouring inventories is only known to the server, so the client asks and
 * the server writes the filter itself; the menu then syncs the slots back like any other edit.
 */
public record ItemSinkImportRequestMessage(ModuleTarget target) implements CustomPacketPayload {

    public static final Type<ItemSinkImportRequestMessage> TYPE =
        new Type<>(LPConstants.rl("item_sink_import_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemSinkImportRequestMessage> STREAM_CODEC =
        StreamCodec.composite(
            ModuleTarget.STREAM_CODEC, ItemSinkImportRequestMessage::target,
            ItemSinkImportRequestMessage::new);

    public static void handle(ItemSinkImportRequestMessage message, IPayloadContext context) {
        final ModuleItemSink module = message.target.resolve(context.player(), ModuleItemSink.class);
        if (module == null || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        final List<ItemIdentifier> items = module.getAdjacentInventoriesItems()
            .limit(module.filterInventory.getContainerSize())
            .toList();
        for (int slot = 0; slot < module.filterInventory.getContainerSize(); slot++) {
            if (slot < items.size()) {
                module.filterInventory.setItem(slot, items.get(slot).makeStack(1));
            } else {
                module.filterInventory.setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
