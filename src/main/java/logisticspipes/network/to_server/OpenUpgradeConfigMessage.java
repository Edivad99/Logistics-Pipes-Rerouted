package logisticspipes.network.to_server;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.network.abstractguis.UpgradeCoordinatesGuiProvider;
import logisticspipes.pipes.upgrades.IConfigPipeUpgrade;
import logisticspipes.pipes.upgrades.IPipeUpgrade;
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
        final UpgradeCoordinatesGuiProvider gui = configurable.getGUI();
        if (gui != null) {
            gui.setSlot(slot).setLPPos(slot.getManager().getPipePosition()).open(context.player());
        }
    }
}
