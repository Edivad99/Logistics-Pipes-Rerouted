package logisticspipes.network.to_client.crafting;

import java.util.List;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.gui.popup.GuiRecipeImport;
import logisticspipes.utils.gui.ISubGuiController;

/**
 * Which ingredient to preselect in each slot of the recipe being imported.
 *
 * <p>An empty entry means the network has nothing to offer for that slot, and whatever the player
 * picked stays.
 */
public record LikelyRecipeComponentsMessage(List<Optional<Integer>> choices) implements CustomPacketPayload {

    public static final Type<LikelyRecipeComponentsMessage> TYPE =
            new Type<>(LPConstants.rl("likely_recipe_components"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LikelyRecipeComponentsMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.optional(ByteBufCodecs.VAR_INT).apply(ByteBufCodecs.list()),
                    LikelyRecipeComponentsMessage::choices,
                    LikelyRecipeComponentsMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LikelyRecipeComponentsMessage message, IPayloadContext context) {
        if (Minecraft.getInstance().screen instanceof ISubGuiController controller) {
            final GuiRecipeImport gui = controller.findSubGui(GuiRecipeImport.class);
            if (gui != null) {
                gui.selectComponents(message.choices);
            }
        }
    }
}
