package logisticspipes.proxy.interfaces;

import javax.annotation.Nonnull;
import logisticspipes.api.ILPPipeConfigTool;
import net.minecraft.world.item.ItemStack;

public interface ILPPipeConfigToolWrapper {

	ILPPipeConfigTool getWrappedTool(@Nonnull ItemStack stack);
}
