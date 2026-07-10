package logisticspipes.items;

import javax.annotation.Nonnull;
import logisticspipes.interfaces.IItemAdvancedExistance;
import net.minecraft.world.item.ItemStack;

public class LogisticsBrokenItem extends LogisticsItem implements IItemAdvancedExistance {

	private static final String PREFIX = "tooltip.brokenItem.";

	@Override
	public boolean canExistInNormalInventory(@Nonnull ItemStack stack) {
		return false;
	}

	@Override
	public boolean canExistInWorld(@Nonnull ItemStack stack) {
		return false;
	}

//	@Override
//	@OnlyIn(Dist.CLIENT)
//	public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flagIn) {
//		tooltip.add(net.minecraft.network.chat.Component.literal(" - " + TextUtil.translate(LogisticsBrokenItem.PREFIX + "1")));
//		tooltip.add(net.minecraft.network.chat.Component.literal(" - " + TextUtil.translate(LogisticsBrokenItem.PREFIX + "2")));
//		tooltip.add(net.minecraft.network.chat.Component.literal("    " + TextUtil.translate(LogisticsBrokenItem.PREFIX + "3")));
//	}
}
