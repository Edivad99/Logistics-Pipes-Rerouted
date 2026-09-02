package logisticspipes.world.item;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import logisticspipes.interfaces.IItemAdvancedExistance;
import logisticspipes.world.item.component.LPDataComponents;
import network.rs485.logisticspipes.util.TextUtil;

/**
 * A card carrying a frequency, written by an inventory system pipe.
 *
 * <p>Until 1.12.2 this item was two cards in one, told apart by the stack's damage value the way
 * metadata used to work. The security card is {@link LogisticsSecurityCard} now: what a stack is
 * belongs in the registry, not in a field that reads as durability.
 */
public class LogisticsItemCard extends LogisticsItem implements IItemAdvancedExistance {

    public LogisticsItemCard(Properties properties) {
        super(properties.stacksTo(64));
    }

    /** Extra lines shown while shift is held, after the id. */
    protected void appendDetails(ItemStack stack, UUID id, Consumer<Component> tooltipAdder) {
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay,
        Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        if (!stack.has(LPDataComponents.UUID)) {
            tooltipAdder.accept(Component.literal(TextUtil.translate("tooltip.logisticsItemCard")));
            return;
        }
        if (Minecraft.getInstance().hasShiftDown()) {
            final UUID id = Objects.requireNonNull(stack.get(LPDataComponents.UUID));
            tooltipAdder.accept(Component.literal("Id: " + id));
            appendDetails(stack, id, tooltipAdder);
        }
    }

    @Override
    public boolean canExistInNormalInventory(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canExistInWorld(ItemStack stack) {
        return true;
    }
}
