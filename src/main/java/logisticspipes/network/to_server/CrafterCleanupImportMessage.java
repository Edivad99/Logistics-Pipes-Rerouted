package logisticspipes.network.to_server;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.network.ModuleTarget;

/**
 * The player asked the crafting module to fill its cleanup filter from the recipe it holds.
 */
public record CrafterCleanupImportMessage(ModuleTarget target) implements CustomPacketPayload {

    public static final Type<CrafterCleanupImportMessage> TYPE =
            new Type<>(LPConstants.rl("crafter_cleanup_import"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrafterCleanupImportMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, CrafterCleanupImportMessage::target,
                    CrafterCleanupImportMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CrafterCleanupImportMessage message, IPayloadContext context) {
        final ModuleCrafter module = message.target.resolve(context.player(), ModuleCrafter.class);
        if (module != null) {
            module.importCleanup();
        }
    }
}
