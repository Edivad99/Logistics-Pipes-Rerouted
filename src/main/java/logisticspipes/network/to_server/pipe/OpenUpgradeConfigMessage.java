package logisticspipes.network.to_server.pipe;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.to_client.pipe.UpgradeConfigPopupMessage;
import logisticspipes.pipes.upgrades.IConfigPipeUpgrade;
import logisticspipes.pipes.upgrades.IPipeUpgrade;
import logisticspipes.network.TargetLookup;
import logisticspipes.utils.gui.UpgradeSlot;

/**
 * The player clicked the config button on an upgrade: open its popup.
 *
 * <p>Opening happens server-side, the way every LP GUI opens.
 */
public record OpenUpgradeConfigMessage(int slot) implements CustomPacketPayload {

    public static final Type<OpenUpgradeConfigMessage> TYPE =
            new Type<>(LPConstants.rl("open_upgrade_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenUpgradeConfigMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, OpenUpgradeConfigMessage::slot,
                    OpenUpgradeConfigMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenUpgradeConfigMessage message, IPayloadContext context) {
        final UpgradeSlot slot = TargetLookup.slotIn(context.player(), message.slot, UpgradeSlot.class);
        if (slot == null) {
            return;
        }
        final IPipeUpgrade upgrade = slot.getUpgrade();
        if (!(upgrade instanceof IConfigPipeUpgrade configurable)) {
            return;
        }
        if (context.player() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new UpgradeConfigPopupMessage(
                    configurable.getConfigPopup(),
                    slot.getManager().getPipePosition().getBlockPos(),
                    message.slot));
        }
    }
}
