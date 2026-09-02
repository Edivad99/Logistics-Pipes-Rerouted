package logisticspipes.network.to_client;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.ICraftingRecipeGrid;
import logisticspipes.network.TargetLookup;
import logisticspipes.utils.item.ItemIdentifier;

/**
 * Which recipe the crafting grid currently produces.
 *
 * <p>Worked out by the server whenever the grid changes, because deciding which of several matching
 * recipes wins depends on state the client does not have.
 *
 * <p>Empty when the grid matches no recipe at all, which the old packet expressed by writing a null
 * item identifier.
 */
public record CraftingTargetMessage(BlockPos pos, Optional<ItemIdentifier> target)
        implements CustomPacketPayload {

    public static final Type<CraftingTargetMessage> TYPE =
            new Type<>(LPConstants.rl("crafting_target"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingTargetMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, CraftingTargetMessage::pos,
                    ByteBufCodecs.optional(ItemIdentifier.STREAM_CODEC), CraftingTargetMessage::target,
                    CraftingTargetMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CraftingTargetMessage message, IPayloadContext context) {
        final ICraftingRecipeGrid grid =
                TargetLookup.blockEntityOrPipeAt(context.player(), message.pos, ICraftingRecipeGrid.class);
        if (grid != null) {
            grid.setTargetType(message.target.orElse(null));
            grid.cacheRecipe();
        }
    }
}
