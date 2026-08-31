package logisticspipes.network.to_client;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.network.ModuleTarget;

/**
 * The amount the server settled on for one fluid slot, sent back to the player who changed it.
 */
public record FluidCraftingAmountMessage(ModuleTarget target, int slot, int amount)
        implements CustomPacketPayload {

    public static final Type<FluidCraftingAmountMessage> TYPE =
            new Type<>(LPConstants.rl("fluid_crafting_amount"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidCraftingAmountMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, FluidCraftingAmountMessage::target,
                    ByteBufCodecs.VAR_INT, FluidCraftingAmountMessage::slot,
                    ByteBufCodecs.VAR_INT, FluidCraftingAmountMessage::amount,
                    FluidCraftingAmountMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FluidCraftingAmountMessage message, IPayloadContext context) {
        final ModuleCrafter module = message.target.resolve(context.player(), ModuleCrafter.class);
        if (module != null) {
            module.liquidAmounts.set(message.slot, message.amount);
        }
    }
}
