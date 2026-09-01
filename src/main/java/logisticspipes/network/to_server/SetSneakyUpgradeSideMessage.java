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
import logisticspipes.pipes.upgrades.SneakyUpgradeConfig;
import logisticspipes.utils.gui.UpgradeSlot;

/**
 * The side picked in the sneaky upgrade's configuration popup.
 *
 * <p>Empty means the default side, the same {@code Optional} the sneaky modules use.
 */
public record SetSneakyUpgradeSideMessage(int slot, Optional<Direction> side) implements CustomPacketPayload {

    public static final Type<SetSneakyUpgradeSideMessage> TYPE =
            new Type<>(LPConstants.rl("set_sneaky_upgrade_side"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetSneakyUpgradeSideMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SetSneakyUpgradeSideMessage::slot,
                    ByteBufCodecs.optional(Direction.STREAM_CODEC), SetSneakyUpgradeSideMessage::side,
                    SetSneakyUpgradeSideMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetSneakyUpgradeSideMessage message, IPayloadContext context) {
        final UpgradeSlot slot = TargetLookup.slotIn(context.player(), message.slot, UpgradeSlot.class);
        if (slot == null) {
            return;
        }
        final ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return;
        }
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, customData -> {
            final var tag = customData.copyTag();
            tag.putString(SneakyUpgradeConfig.SIDE_KEY,
                    SneakyUpgradeConfig.Sides.getNameForDirection(message.side.orElse(null)));
            return CustomData.of(tag);
        });
        slot.set(stack);
    }
}
