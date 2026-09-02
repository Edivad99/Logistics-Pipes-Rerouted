package logisticspipes.network.to_server.crafting;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.ICraftingRecipeGrid;
import logisticspipes.network.TargetLookup;
import logisticspipes.network.to_client.crafting.LikelyRecipeComponentsMessage;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.request.RecipeComponentGuesser;
import logisticspipes.utils.item.ItemIdentifier;

/**
 * Asks which ingredient to use in each slot of a recipe being imported.
 *
 * <p>A recipe viewer offers several interchangeable ingredients per slot; only the network knows
 * which of them it can actually supply, so the choice is made server side.
 */
public record FindLikelyRecipeComponentsMessage(BlockPos pos, List<List<ItemIdentifier>> candidates)
        implements CustomPacketPayload {

    public static final Type<FindLikelyRecipeComponentsMessage> TYPE =
            new Type<>(LPConstants.rl("find_likely_recipe_components"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FindLikelyRecipeComponentsMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, FindLikelyRecipeComponentsMessage::pos,
                    ItemIdentifier.STREAM_CODEC.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list()),
                    FindLikelyRecipeComponentsMessage::candidates,
                    FindLikelyRecipeComponentsMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FindLikelyRecipeComponentsMessage message, IPayloadContext context) {
        final ICraftingRecipeGrid grid =
                TargetLookup.blockEntityOrPipeAt(context.player(), message.pos, ICraftingRecipeGrid.class);
        if (grid == null || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        final CoreRoutedPipe pipe = grid.getAttachedPipe();
        if (pipe != null) {
            PacketDistributor.sendToPlayer(player,
                    new LikelyRecipeComponentsMessage(RecipeComponentGuesser.choose(pipe, message.candidates)));
        }
    }
}
