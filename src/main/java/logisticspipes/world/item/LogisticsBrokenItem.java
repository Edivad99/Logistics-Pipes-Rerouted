package logisticspipes.world.item;

import net.minecraft.world.item.ItemStack;

import logisticspipes.interfaces.IItemAdvancedExistence;

public class LogisticsBrokenItem extends LogisticsItem implements IItemAdvancedExistence {

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
}
