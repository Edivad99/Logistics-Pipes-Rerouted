package logisticspipes.network.to_server.config;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.Translations;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.item.component.HUDComponent;
import logisticspipes.world.item.component.HudSetting;
import logisticspipes.world.item.component.LPDataComponents;

/**
 * The player ticked one of the HUD glasses' checkboxes.
 *
 * <p>The setting is named rather than numbered: it used to travel as the checkbox's own widget id,
 * which the server turned back into behaviour with a {@code switch} over 0 to 5. Moving a checkbox
 * in the screen would have changed what the message meant.
 *
 * @param slot where the glasses are in the player's inventory; the server reads the setting off
 *             the item, not off the player
 */
public record SetHudSettingMessage(int slot, HudSetting setting, boolean state)
    implements CustomPacketPayload {

    public static final Type<SetHudSettingMessage> TYPE = new Type<>(LPConstants.rl("set_hud_setting"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetHudSettingMessage> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetHudSettingMessage::slot,
            NeoForgeStreamCodecs.enumCodec(HudSetting.class),
            SetHudSettingMessage::setting,
            ByteBufCodecs.BOOL, SetHudSettingMessage::state,
            SetHudSettingMessage::new);

    public static void handle(SetHudSettingMessage message, IPayloadContext context) {
        final Player player = context.player();
        if (message.slot < 0 || message.slot >= player.getInventory().getContainerSize()) {
            return;
        }
        final ItemStack glasses = player.getInventory().getItem(message.slot);
        if (!glasses.is(LPItems.HUD_GLASSES.get())) {
            return;
        }
        glasses.set(LPDataComponents.HUD, glasses.getOrDefault(LPDataComponents.HUD, HUDComponent.DEFAULT)
            .with(message.setting, message.state));
        player.sendOverlayMessage(Component.translatable(Translations.Chat.hudSetting(message.setting, message.state)));
        player.inventoryMenu.broadcastChanges();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
