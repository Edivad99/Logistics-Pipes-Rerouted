package logisticspipes.network.to_server.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.ICraftingRecipeGrid;
import logisticspipes.network.TargetLookup;

/**
 * The player stepped through the recipes the crafting grid could produce.
 *
 * @param down which of the two arrows was pressed
 */
public record CycleCraftingRecipeMessage(BlockPos pos, boolean down) implements CustomPacketPayload {

    public static final Type<CycleCraftingRecipeMessage> TYPE =
            new Type<>(LPConstants.rl("cycle_crafting_recipe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CycleCraftingRecipeMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, CycleCraftingRecipeMessage::pos,
                    ByteBufCodecs.BOOL, CycleCraftingRecipeMessage::down,
                    CycleCraftingRecipeMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CycleCraftingRecipeMessage message, IPayloadContext context) {
        final ICraftingRecipeGrid grid =
                TargetLookup.blockEntityOrPipeAt(context.player(), message.pos, ICraftingRecipeGrid.class);
        if (grid != null) {
            grid.cycleRecipe(message.down);
        }
    }
}
