package logisticspipes.network.to_server.crafting;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.ICraftingRecipeGrid;
import logisticspipes.network.TargetLookup;

/**
 * The player dragged a recipe out of a recipe viewer onto the crafting grid.
 *
 * <p>Nine slots, some of them empty, which is why the stacks travel with the optional codec.
 */
public record ImportCraftingRecipeMessage(BlockPos pos, List<ItemStack> contents)
        implements CustomPacketPayload {

    public static final Type<ImportCraftingRecipeMessage> TYPE =
            new Type<>(LPConstants.rl("import_crafting_recipe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ImportCraftingRecipeMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ImportCraftingRecipeMessage::pos,
                    ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
                    ImportCraftingRecipeMessage::contents,
                    ImportCraftingRecipeMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ImportCraftingRecipeMessage message, IPayloadContext context) {
        final ICraftingRecipeGrid grid =
                TargetLookup.blockEntityOrPipeAt(context.player(), message.pos, ICraftingRecipeGrid.class);
        if (grid == null) {
            return;
        }
        final NonNullList<ItemStack> contents = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(contents.size(), message.contents.size()); i++) {
            contents.set(i, message.contents.get(i));
        }
        grid.handleRecipeViewerImport(contents);
    }
}
