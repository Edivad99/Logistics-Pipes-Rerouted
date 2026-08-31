package logisticspipes.network.to_server;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.network.ModuleTarget;

/**
 * The player nudged a fluid amount in the crafting GUI: apply the delta server-side.
 */
public record ChangeFluidCraftingAmountMessage(ModuleTarget target, int slot, int change)
        implements CustomPacketPayload {

    public static final Type<ChangeFluidCraftingAmountMessage> TYPE =
            new Type<>(LPConstants.rl("change_fluid_crafting_amount"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChangeFluidCraftingAmountMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, ChangeFluidCraftingAmountMessage::target,
                    ByteBufCodecs.VAR_INT, ChangeFluidCraftingAmountMessage::slot,
                    ByteBufCodecs.VAR_INT, ChangeFluidCraftingAmountMessage::change,
                    ChangeFluidCraftingAmountMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChangeFluidCraftingAmountMessage message, IPayloadContext context) {
        final ModuleCrafter module = message.target.resolve(context.player(), ModuleCrafter.class);
        if (module != null) {
            module.changeFluidAmount(message.change, message.slot, context.player());
        }
    }
}
