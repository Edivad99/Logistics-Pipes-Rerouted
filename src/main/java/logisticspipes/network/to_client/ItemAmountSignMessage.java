package logisticspipes.network.to_client;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.pipes.signs.IPipeSign;
import logisticspipes.pipes.signs.ItemAmountPipeSign;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * What an item amount sign shows: which item, and how many.
 *
 * <p>The side used to travel as the ordinal of a {@code Direction} in an int field named
 * {@code integer}, with the amount in one named {@code integer2}.
 */
public record ItemAmountSignMessage(
        BlockPos pos,
        Direction side,
        int amount,
        Optional<ItemIdentifierStack> item
) implements CustomPacketPayload {

    public static final Type<ItemAmountSignMessage> TYPE =
            new Type<>(LPConstants.rl("item_amount_sign"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemAmountSignMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ItemAmountSignMessage::pos,
                    Direction.STREAM_CODEC, ItemAmountSignMessage::side,
                    ByteBufCodecs.VAR_INT, ItemAmountSignMessage::amount,
                    ByteBufCodecs.optional(ItemIdentifierStack.STREAM_CODEC), ItemAmountSignMessage::item,
                    ItemAmountSignMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ItemAmountSignMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe container =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (container == null || !container.isInitialized() || !(container.pipe instanceof CoreRoutedPipe pipe)) {
            return;
        }
        final IPipeSign sign = pipe.getPipeSign(message.side);
        if (sign instanceof ItemAmountPipeSign amountSign) {
            amountSign.amount = message.amount;
            amountSign.itemTypeInv.setItem(0, message.item.orElse(null));
        }
    }
}
