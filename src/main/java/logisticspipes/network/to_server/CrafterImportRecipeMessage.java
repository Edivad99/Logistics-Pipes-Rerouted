package logisticspipes.network.to_server;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.network.ModuleTarget;

/**
 * The player asked the crafting module to read a recipe out of an adjacent crafting table.
 *
 * <p>Only the server can see what sits next to the pipe, so the client's own
 * {@code importFromCraftingTable} does nothing but send this and wait for the resulting module
 * update.
 */
public record CrafterImportRecipeMessage(ModuleTarget target) implements CustomPacketPayload {

    public static final Type<CrafterImportRecipeMessage> TYPE =
            new Type<>(LPConstants.rl("crafter_import_recipe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrafterImportRecipeMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, CrafterImportRecipeMessage::target,
                    CrafterImportRecipeMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CrafterImportRecipeMessage message, IPayloadContext context) {
        final ModuleCrafter module = message.target.resolve(context.player(), ModuleCrafter.class);
        if (module != null) {
            module.importFromCraftingTable(context.player());
        }
    }
}
