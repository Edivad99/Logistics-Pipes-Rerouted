package logisticspipes.items;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import logisticspipes.LogisticsPipesDataComponents;
import logisticspipes.interfaces.IItemAdvancedExistance;
import logisticspipes.proxy.SimpleServiceLocator;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import network.rs485.logisticspipes.util.TextUtil;

public class LogisticsItemCard extends LogisticsItem implements IItemAdvancedExistance {

	public static final int FREQ_CARD = 0;
	public static final int SEC_CARD = 1;

	public LogisticsItemCard() {
		// hasSubtypes removed in 1.20.1 — item variants handled via DamageValue or separate items
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
		super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
		if (stack.has(LogisticsPipesDataComponents.UUID)) {
			UUID uuid = Objects.requireNonNull(stack.get(LogisticsPipesDataComponents.UUID));
			if (stack.getDamageValue() == LogisticsItemCard.FREQ_CARD) {
				tooltipComponents.add(Component.literal("Freq. Card"));
			} else if (stack.getDamageValue() == LogisticsItemCard.SEC_CARD) {
				tooltipComponents.add(Component.literal("Sec. Card"));
			}
			if (Screen.hasShiftDown()) {
				tooltipComponents.add(Component.literal("Id: " + uuid));
				if (stack.getDamageValue() == LogisticsItemCard.SEC_CARD) {
					tooltipComponents.add(Component.literal("Authorization: " + (SimpleServiceLocator.securityStationManager.isAuthorized(uuid) ? "Authorized" : "Unauthorized")));
				}
			}
		}
		else {
			tooltipComponents.add(Component.literal(TextUtil.translate("tooltip.logisticsItemCard")));
		}
	}

	// getShareTag() removed in 1.20 — NBT always shared now
	@Deprecated public boolean getShareTag__REMOVED() {
		return true;
	}

	@Override
	public int getMaxStackSize(@Nonnull ItemStack stack) {
		return 64;
	}

	@Override
	public boolean canExistInNormalInventory(@Nonnull ItemStack stack) {
		return true;
	}

	@Override
	public boolean canExistInWorld(@Nonnull ItemStack stack) {
		return stack.getDamageValue() != LogisticsItemCard.SEC_CARD;
	}
}
