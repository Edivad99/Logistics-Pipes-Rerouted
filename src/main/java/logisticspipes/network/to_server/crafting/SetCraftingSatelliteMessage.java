package logisticspipes.network.to_server.crafting;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.network.ModuleTarget;

/**
 * The player picked a satellite in the crafting pipe's selection popup.
 *
 * <p>The slot number doubles as the kind of satellite, the way the crafting module has always
 * encoded it: 0 is the item satellite, 10-19 the advanced item ones, 100 the fluid satellite and
 * 110-120 the advanced fluid ones.
 */
/**
 * Which satellite a crafting module should send to, or none.
 *
 * <p>Empty means the player pressed unset: the module keeps a satellite until told otherwise, so
 * clearing one has to be sayable.
 */
public record SetCraftingSatelliteMessage(ModuleTarget target, int slot, Optional<UUID> satellite)
        implements CustomPacketPayload {

    public static final Type<SetCraftingSatelliteMessage> TYPE =
            new Type<>(LPConstants.rl("set_crafting_satellite"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetCraftingSatelliteMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, SetCraftingSatelliteMessage::target,
                    ByteBufCodecs.VAR_INT, SetCraftingSatelliteMessage::slot,
                    ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), SetCraftingSatelliteMessage::satellite,
                    SetCraftingSatelliteMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetCraftingSatelliteMessage message, IPayloadContext context) {
        final ModuleCrafter module = message.target.resolve(context.player(), ModuleCrafter.class);
        if (module == null) {
            return;
        }
        final UUID satellite = message.satellite.orElse(null);
        if (message.slot == 0) {
            module.setSatelliteUUID(satellite);
        } else if (message.slot >= 10 && message.slot < 20) {
            module.setAdvancedSatelliteUUID(message.slot - 10, satellite);
        } else if (message.slot == 100) {
            module.setFluidSatelliteUUID(satellite);
        } else if (message.slot >= 110 && message.slot <= 120) {
            module.setAdvancedFluidSatelliteUUID(message.slot - 110, satellite);
        }
    }
}
