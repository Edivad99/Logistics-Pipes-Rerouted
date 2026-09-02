package logisticspipes.network.to_client.module;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.to_server.module.ItemSinkImportRequestMessage;
import logisticspipes.utils.item.ItemIdentifier;

import network.rs485.logisticspipes.gui.module.ItemSinkGui;

/**
 * What the server found in the inventories next to the pipe, in answer to
 * {@link ItemSinkImportRequestMessage}.
 *
 * <p>The items go to the screen that asked rather than to the module: the GUI edits a filter
 * overlay the player can still cancel, and only writes it back to the module on save. The module
 * the message came from is therefore not addressed -- the open screen is the whole recipient.
 */
public record ItemSinkImportedItemsMessage(List<ItemIdentifier> items) implements CustomPacketPayload {

    public static final Type<ItemSinkImportedItemsMessage> TYPE =
            new Type<>(LPConstants.rl("item_sink_imported_items"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemSinkImportedItemsMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ItemIdentifier.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    ItemSinkImportedItemsMessage::items,
                    ItemSinkImportedItemsMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ItemSinkImportedItemsMessage message, IPayloadContext context) {
        if (Minecraft.getInstance().screen instanceof ItemSinkGui gui) {
            gui.importFromInventory(message.items);
        }
    }
}
