package logisticspipes.world.item;

import java.util.List;
import java.util.function.Consumer;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import logisticspipes.interfaces.IItemAdvancedExistance;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.world.item.component.LPDataComponents;
import network.rs485.logisticspipes.util.TextUtil;

public class LogisticsItemCard extends LogisticsItem implements IItemAdvancedExistance {

    public static final int FREQ_CARD = 0;
    public static final int SEC_CARD = 1;

    public LogisticsItemCard(Properties properties) {
        super(properties.stacksTo(64));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay,
        Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        if (stack.has(LPDataComponents.UUID)) {
            UUID uuid = Objects.requireNonNull(stack.get(LPDataComponents.UUID));
            if (stack.getDamageValue() == LogisticsItemCard.FREQ_CARD) {
                tooltipAdder.accept(Component.literal("Freq. Card"));
            } else if (stack.getDamageValue() == LogisticsItemCard.SEC_CARD) {
                tooltipAdder.accept(Component.literal("Sec. Card"));
            }
            if (Minecraft.getInstance().hasShiftDown()) {
                tooltipAdder.accept(Component.literal("Id: " + uuid));
                if (stack.getDamageValue() == LogisticsItemCard.SEC_CARD) {
                    tooltipAdder.accept(Component.literal(
                        "Authorization: " + (SimpleServiceLocator.securityStationManager.isAuthorized(uuid) ?
                            "Authorized" :
                            "Unauthorized")));
                }
            }
        } else {
            tooltipAdder.accept(Component.literal(TextUtil.translate("tooltip.logisticsItemCard")));
        }
    }

    // getShareTag() removed in 1.20 — NBT always shared now
    @Deprecated
    public boolean getShareTag__REMOVED() {
        return true;
    }

    @Override
    public boolean canExistInNormalInventory(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canExistInWorld(ItemStack stack) {
        return stack.getDamageValue() != LogisticsItemCard.SEC_CARD;
    }
}
