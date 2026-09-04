package logisticspipes.network.to_client.pipe;

import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.client.gui.popup.DisconnectionConfigurationPopup;
import logisticspipes.client.gui.popup.SneakyConfigurationPopup;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.util.DoubleCoordinates;
import logisticspipes.utils.gui.ISubGuiController;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.gui.UpgradeSlot;
import network.rs485.logisticspipes.world.WorldCoordinatesWrapper;

/**
 * Opens the settings popup of an upgrade the player clicked.
 *
 * <p>A popup is not a menu: it sits on top of the screen the player already has open, so it does
 * not open a container of its own. Only the server knows which upgrade is in the slot, hence the
 * round trip; everything the popup shows the client can work out for itself.
 */
public record UpgradeConfigPopupMessage(Kind kind, BlockPos pipePos, int slotIndex)
        implements CustomPacketPayload {

    /** Which upgrade asked, and so which popup to build. */
    public enum Kind {
        SNEAKY,
        DISCONNECTION,
    }

    public static final Type<UpgradeConfigPopupMessage> TYPE =
            new Type<>(LPConstants.rl("upgrade_config_popup"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpgradeConfigPopupMessage> STREAM_CODEC =
            StreamCodec.composite(
                    NeoForgeStreamCodecs.<RegistryFriendlyByteBuf, Kind>enumCodec(Kind.class),
                    UpgradeConfigPopupMessage::kind,
                    BlockPos.STREAM_CODEC, UpgradeConfigPopupMessage::pipePos,
                    ByteBufCodecs.VAR_INT, UpgradeConfigPopupMessage::slotIndex,
                    UpgradeConfigPopupMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpgradeConfigPopupMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe container =
                TargetLookup.blockEntityAt(context.player(), message.pipePos, LogisticsTileGenericPipe.class);
        final UpgradeSlot slot = TargetLookup.slotIn(context.player(), message.slotIndex, UpgradeSlot.class);
        if (container == null || slot == null || !(container.pipe instanceof CoreRoutedPipe pipe)) {
            return;
        }
        if (!(Minecraft.getInstance().screen instanceof ISubGuiController controller)) {
            return;
        }
        controller.pushSubGui(popupFor(message.kind, container, pipe, slot));
    }

    private static SubGuiScreen popupFor(Kind kind, LogisticsTileGenericPipe container, CoreRoutedPipe pipe,
            UpgradeSlot slot) {
        return kind == Kind.DISCONNECTION
                ? new DisconnectionConfigurationPopup(pipe, slot)
                : new SneakyConfigurationPopup(extractableSides(container), slot);
    }

    /**
     * The neighbours a sneaky upgrade can be pointed at: the inventories first, and if there are
     * none, every neighbour, so the popup is never empty.
     */
    private static List<DoubleCoordinates> extractableSides(LogisticsTileGenericPipe container) {
        final List<DoubleCoordinates> inventories = new WorldCoordinatesWrapper(container).connectedTileEntities()
                .stream()
                .filter(neighbor -> SimpleServiceLocator.pipeInformationManager.isNotAPipe(neighbor.getTileEntity()))
                .map(neighbor -> new DoubleCoordinates(neighbor.getTileEntity()))
                .collect(Collectors.toList());
        if (!inventories.isEmpty()) {
            return inventories;
        }
        return new WorldCoordinatesWrapper(container).connectedTileEntities().stream()
                .map(neighbor -> new DoubleCoordinates(neighbor.getTileEntity()))
                .collect(Collectors.toList());
    }
}
