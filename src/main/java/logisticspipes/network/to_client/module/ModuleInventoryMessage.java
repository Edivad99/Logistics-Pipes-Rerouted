package logisticspipes.network.to_client.module;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.IModuleInventoryReceive;
import logisticspipes.network.ModuleTarget;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * What a module's filter inventory holds, for the players watching its HUD.
 *
 * <p>Sent whenever the inventory changes and again to each player as they start watching, which
 * is what makes a dropped one harmless.
 */
public record ModuleInventoryMessage(ModuleTarget target, List<ItemIdentifierStack> contents)
        implements CustomPacketPayload {

    public static final Type<ModuleInventoryMessage> TYPE =
            new Type<>(LPConstants.rl("module_inventory"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ModuleInventoryMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, ModuleInventoryMessage::target,
                    ItemIdentifierStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    ModuleInventoryMessage::contents,
                    ModuleInventoryMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ModuleInventoryMessage message, IPayloadContext context) {
        final IModuleInventoryReceive module =
                message.target.resolve(context.player(), IModuleInventoryReceive.class);
        if (module != null) {
            module.handleInvContent(message.contents);
        }
    }
}
