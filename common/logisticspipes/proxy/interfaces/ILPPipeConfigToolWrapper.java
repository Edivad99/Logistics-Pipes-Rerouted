package logisticspipes.proxy.interfaces;

import logisticspipes.api.ILPPipeConfigTool;
import net.minecraft.world.item.ItemStack;

public interface ILPPipeConfigToolWrapper {

	ILPPipeConfigTool getWrappedTool(ItemStack stack);
}
