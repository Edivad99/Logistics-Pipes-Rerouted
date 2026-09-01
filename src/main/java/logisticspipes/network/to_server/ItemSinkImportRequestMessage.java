package logisticspipes.network.to_server;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.modules.ModuleItemSink;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.to_client.ItemSinkImportedItemsMessage;
import logisticspipes.utils.item.ItemIdentifier;

/**
 * The player pressed "import" in the item sink's GUI.
 *
 * <p>What sits in the neighbouring inventories is only known to the server, so the client asks and
 * the server answers with {@link ItemSinkImportedItemsMessage}.
 */
public record ItemSinkImportRequestMessage(ModuleTarget target) implements CustomPacketPayload {

    public static final Type<ItemSinkImportRequestMessage> TYPE =
            new Type<>(LPConstants.rl("item_sink_import_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemSinkImportRequestMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, ItemSinkImportRequestMessage::target,
                    ItemSinkImportRequestMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ItemSinkImportRequestMessage message, IPayloadContext context) {
        final ModuleItemSink module = message.target.resolve(context.player(), ModuleItemSink.class);
        if (module == null || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        final List<ItemIdentifier> items = module.getAdjacentInventoriesItems()
                .limit(module.filterInventory.getContainerSize())
                .toList();
        PacketDistributor.sendToPlayer(player, new ItemSinkImportedItemsMessage(items));
    }
}
