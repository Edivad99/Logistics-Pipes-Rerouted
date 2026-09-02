package logisticspipes.network.to_server.config;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

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
import logisticspipes.hud.HUDConfig;
import logisticspipes.interfaces.IHUDConfig;
import logisticspipes.world.item.LPItems;

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

    /** One switch on the HUD glasses, and the message shown when it is flipped. */
    public enum HudSetting {
        CHASSIS(IHUDConfig::setChassisHUD, IHUDConfig::isChassisHUD, "chassie"),
        CRAFTING(IHUDConfig::setHUDCrafting, IHUDConfig::isHUDCrafting, "crafting"),
        INV_SYS_CON(IHUDConfig::setHUDInvSysCon, IHUDConfig::isHUDInvSysCon, "invsyscon"),
        POWER_JUNCTION(IHUDConfig::setHUDPowerJunction, IHUDConfig::isHUDPowerLevel, "powerjunction"),
        PROVIDER(IHUDConfig::setHUDProvider, IHUDConfig::isHUDProvider, "provider"),
        SATELLITE(IHUDConfig::setHUDSatellite, IHUDConfig::isHUDSatellite, "satellite");

        private final BiConsumer<IHUDConfig, Boolean> setter;
        private final Predicate<IHUDConfig> getter;
        private final String translationKey;

        HudSetting(BiConsumer<IHUDConfig, Boolean> setter, Predicate<IHUDConfig> getter,
                String translationKey) {
            this.setter = setter;
            this.getter = getter;
            this.translationKey = translationKey;
        }

        void apply(IHUDConfig config, boolean state, Player player) {
            setter.accept(config, state);
            player.sendSystemMessage(Component.translatable("lp.hud.config." + translationKey
                    + (getter.test(config) ? ".enabled" : ".disabled")));
        }
    }

    public static final Type<SetHudSettingMessage> TYPE = new Type<>(LPConstants.rl("set_hud_setting"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetHudSettingMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SetHudSettingMessage::slot,
                    NeoForgeStreamCodecs.<RegistryFriendlyByteBuf, HudSetting>enumCodec(HudSetting.class),
                    SetHudSettingMessage::setting,
                    ByteBufCodecs.BOOL, SetHudSettingMessage::state,
                    SetHudSettingMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetHudSettingMessage message, IPayloadContext context) {
        final Player player = context.player();
        if (message.slot < 0 || message.slot >= player.getInventory().getContainerSize()) {
            return;
        }
        final ItemStack glasses = player.getInventory().getItem(message.slot);
        if (!glasses.is(LPItems.HUD_GLASSES.get())) {
            return;
        }
        message.setting.apply(new HUDConfig(glasses), message.state, player);
        player.inventoryMenu.broadcastChanges();
    }
}
