/**
 * Copyright (c) Krapht, 2011
 * <p>
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.world.item;

import java.util.List;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import network.rs485.logisticspipes.util.TextUtil;

public class LogisticsItem extends Item {

    public LogisticsItem(Properties properties) {
        super(properties);
    }

    /**
     * Adds all keys from the translation file in the format:
     * item.className.tip([0-9]*) Tips start from 1 and increment. Sparse rows
     * should be left empty (ie empty line must still have a key present) Shift
     * shows full tooltip, without it, you just get the first line.
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
        TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        if (addShiftInfo()) {
            TextUtil.addTooltipInformation(stack, tooltipComponents, Screen.hasShiftDown());
        }
    }

    public boolean addShiftInfo() {
        return true;
    }

}
