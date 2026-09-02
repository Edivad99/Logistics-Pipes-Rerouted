package logisticspipes.network.bidirectional;

import java.util.BitSet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.IFuzzySlot;

import network.rs485.logisticspipes.util.FuzzyFlag;

/**
 * How a slot matches items: exactly, or ignoring some of what tells two stacks apart.
 *
 * <p>The same message either way. The screen sends it when the player flips a flag, and the
 * container sends it back to everyone watching when the flags change server-side; applying it is
 * one call, and it does not matter which side is doing the applying.
 *
 * <p>The four flags are named rather than sent as the {@link BitSet} that holds them, for the same
 * reason as in {@code DictResource}: a bit index says nothing, and a fourth flag added later is
 * easy to forget.
 */
public record FuzzySlotFlagsMessage(int slotId, boolean useOreDict, boolean ignoreDamage,
        boolean ignoreNbt, boolean useOreCategory) implements CustomPacketPayload {

    public static final Type<FuzzySlotFlagsMessage> TYPE =
            new Type<>(LPConstants.rl("fuzzy_slot_flags"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FuzzySlotFlagsMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, FuzzySlotFlagsMessage::slotId,
                    ByteBufCodecs.BOOL, FuzzySlotFlagsMessage::useOreDict,
                    ByteBufCodecs.BOOL, FuzzySlotFlagsMessage::ignoreDamage,
                    ByteBufCodecs.BOOL, FuzzySlotFlagsMessage::ignoreNbt,
                    ByteBufCodecs.BOOL, FuzzySlotFlagsMessage::useOreCategory,
                    FuzzySlotFlagsMessage::new);

    public static FuzzySlotFlagsMessage of(int slotId, BitSet flags) {
        return new FuzzySlotFlagsMessage(slotId,
                flags.get(FuzzyFlag.USE_ORE_DICT.getBit()),
                flags.get(FuzzyFlag.IGNORE_DAMAGE.getBit()),
                flags.get(FuzzyFlag.IGNORE_NBT.getBit()),
                flags.get(FuzzyFlag.USE_ORE_CATEGORY.getBit()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FuzzySlotFlagsMessage message, IPayloadContext context) {
        if (context.player().containerMenu == null || message.slotId < 0
                || message.slotId >= context.player().containerMenu.slots.size()) {
            return;
        }
        if (context.player().containerMenu.getSlot(message.slotId) instanceof IFuzzySlot slot) {
            final BitSet flags = new BitSet(FuzzyFlag.values().length);
            flags.set(FuzzyFlag.USE_ORE_DICT.getBit(), message.useOreDict);
            flags.set(FuzzyFlag.IGNORE_DAMAGE.getBit(), message.ignoreDamage);
            flags.set(FuzzyFlag.IGNORE_NBT.getBit(), message.ignoreNbt);
            flags.set(FuzzyFlag.USE_ORE_CATEGORY.getBit(), message.useOreCategory);
            slot.getFuzzyFlags().replaceWith(flags);
        }
    }
}
