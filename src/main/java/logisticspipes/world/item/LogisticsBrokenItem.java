package logisticspipes.world.item;

import net.minecraft.world.item.ItemStack;

import logisticspipes.interfaces.IItemAdvancedExistance;

public class LogisticsBrokenItem extends LogisticsItem implements IItemAdvancedExistance {

    private static final String PREFIX = "tooltip.brokenItem.";

    public LogisticsBrokenItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canExistInNormalInventory(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canExistInWorld(ItemStack stack) {
        return false;
    }

    //	@Override
    //	
    //	public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flagIn) {
    //		tooltip.add(net.minecraft.network.chat.Component.literal(" - " + TextUtil.translate(LogisticsBrokenItem.PREFIX + "1")));
    //		tooltip.add(net.minecraft.network.chat.Component.literal(" - " + TextUtil.translate(LogisticsBrokenItem.PREFIX + "2")));
    //		tooltip.add(net.minecraft.network.chat.Component.literal("    " + TextUtil.translate(LogisticsBrokenItem.PREFIX + "3")));
    //	}
}
