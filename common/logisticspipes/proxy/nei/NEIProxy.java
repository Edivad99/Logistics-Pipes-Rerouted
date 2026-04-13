/*
package logisticspipes.proxy.nei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.api.ItemInfo;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.guihook.IContainerTooltipHandler;
import lombok.SneakyThrows;

import logisticspipes.proxy.interfaces.INEIProxy;
import logisticspipes.utils.ReflectionHelper;
public class NEIProxy implements INEIProxy {

	@Override
	public List<String> getInfoForPosition(Level world, Player player, HitResult objectMouseOver) {
		List<ItemStack> items = ItemInfo.getIdentifierItems(world, player, objectMouseOver);
		if (items.isEmpty()) {
			return new ArrayList<>(0);
		}
		Collections.sort(items, (stack0, stack1) -> stack1.getDamageValue() - stack0.getDamageValue());
		return ItemInfo.getText(items.get(0), world, player, objectMouseOver);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	@SneakyThrows({NoSuchFieldException.class, IllegalAccessException.class})
	public boolean renderItemToolTip(int mousex, int mousey, List<String> msg, ChatFormatting rarityColor, ItemStack stack) {
		if (!(Minecraft.getInstance().currentScreen instanceof AbstractContainerScreen)) {
			return false;
		}
		AbstractContainerScreen window = (AbstractContainerScreen) Minecraft.getInstance().currentScreen;
		List<String> tooltip = new LinkedList<String>();
		Font font = GuiDraw.fontRenderer;

		if (GuiContainerManager.shouldShowTooltip(window)) {
			font = GuiContainerManager.getFontRenderer(stack);
			if (stack != null) {
				tooltip = msg;
			}
			for (IContainerTooltipHandler handler : (List<IContainerTooltipHandler>) ReflectionHelper.getPrivateField(List.class, GuiContainerManager.class, "instanceTooltipHandlers", GuiContainerManager.getManager())) {
				tooltip = handler.handleItemTooltip(window, stack, mousex, mousey, tooltip);
			}
		}
		if (tooltip.size() > 0) {
			tooltip.set(0, tooltip.get(0) + "§h");
		}
		GuiDraw.drawMultilineTip(mousex + 12, mousey - 12, tooltip);
		return true;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public List<String> getItemToolTip(ItemStack stack, Player thePlayer, ITooltipFlag advancedItemTooltips, AbstractContainerScreen screen) {
		return GuiContainerManager.itemDisplayNameMultiline(stack, screen, true);
	}

	@Override
	public ItemStack getItemForPosition(Level world, Player player, HitResult objectMouseOver) {
		List<ItemStack> items = ItemInfo.getIdentifierItems(world, player, objectMouseOver);
		if (items.isEmpty()) {
			return null;
		}
		Collections.sort(items, (stack0, stack1) -> stack1.getDamageValue() - stack0.getDamageValue());
		return items.get(0);
	}
}
*/