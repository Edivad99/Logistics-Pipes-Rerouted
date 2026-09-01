package logisticspipes.network.to_server;

import java.util.Optional;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.upgrades.ConnectionUpgradeConfig;
import logisticspipes.utils.gui.UpgradeSlot;

/**
 * A side toggled in the disconnection upgrade's configuration popup.
 */
public record ToggleDisconnectionUpgradeSideMessage(int slot, Optional<Direction> side)
        implements CustomPacketPayload {

    public static final Type<ToggleDisconnectionUpgradeSideMessage> TYPE =
            new Type<>(LPConstants.rl("toggle_disconnection_upgrade_side"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleDisconnectionUpgradeSideMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ToggleDisconnectionUpgradeSideMessage::slot,
                    ByteBufCodecs.optional(Direction.STREAM_CODEC), ToggleDisconnectionUpgradeSideMessage::side,
                    ToggleDisconnectionUpgradeSideMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleDisconnectionUpgradeSideMessage message, IPayloadContext context) {
        final UpgradeSlot slot = TargetLookup.slotIn(context.player(), message.slot, UpgradeSlot.class);
        if (slot == null) {
            return;
        }
        final ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return;
        }
        final String sideName = ConnectionUpgradeConfig.Sides.getNameForDirection(message.side.orElse(null));
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, customData -> {
            final var tag = customData.copyTag();
            tag.putBoolean(sideName, tag.getBooleanOr(sideName, false));
            return CustomData.of(tag);
        });
        slot.set(stack);
    }
}
