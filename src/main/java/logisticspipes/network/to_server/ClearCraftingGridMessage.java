package logisticspipes.network.to_server;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.ICraftingRecipeGrid;
import logisticspipes.network.TargetLookup;

/**
 * The player emptied the crafting grid.
 */
public record ClearCraftingGridMessage(BlockPos pos) implements CustomPacketPayload {

    public static final Type<ClearCraftingGridMessage> TYPE =
            new Type<>(LPConstants.rl("clear_crafting_grid"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClearCraftingGridMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ClearCraftingGridMessage::pos,
                    ClearCraftingGridMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClearCraftingGridMessage message, IPayloadContext context) {
        final ICraftingRecipeGrid grid =
                TargetLookup.blockEntityOrPipeAt(context.player(), message.pos, ICraftingRecipeGrid.class);
        if (grid != null) {
            grid.getMatrix().clearGrid();
            grid.cacheRecipe();
        }
    }
}
