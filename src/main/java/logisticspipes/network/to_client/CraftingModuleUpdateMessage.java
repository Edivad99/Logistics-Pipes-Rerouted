package logisticspipes.network.to_client;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.modules.ModuleCrafter.ClientSideSatelliteNames;
import logisticspipes.network.ModuleTarget;

/**
 * What the crafting module's GUI shows but cannot work out for itself.
 *
 * <p>Sent when the GUI opens and again whenever the module changes. The satellite names are here
 * rather than their UUIDs because resolving a UUID to a name means walking the routing table, which
 * only exists on the server.
 */
public record CraftingModuleUpdateMessage(
        ModuleTarget target,
        List<Integer> liquidAmounts,
        ClientSideSatelliteNames satelliteNames,
        int priority
) implements CustomPacketPayload {

    public static final Type<CraftingModuleUpdateMessage> TYPE =
            new Type<>(LPConstants.rl("crafting_module_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingModuleUpdateMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, CraftingModuleUpdateMessage::target,
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()),
                    CraftingModuleUpdateMessage::liquidAmounts,
                    ClientSideSatelliteNames.STREAM_CODEC, CraftingModuleUpdateMessage::satelliteNames,
                    ByteBufCodecs.VAR_INT, CraftingModuleUpdateMessage::priority,
                    CraftingModuleUpdateMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CraftingModuleUpdateMessage message, IPayloadContext context) {
        final ModuleCrafter module = message.target.resolve(context.player(), ModuleCrafter.class);
        if (module != null) {
            module.applyUpdate(message.liquidAmounts, message.satelliteNames, message.priority);
        }
    }
}
