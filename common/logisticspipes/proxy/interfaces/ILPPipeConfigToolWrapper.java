package logisticspipes.proxy.interfaces;

import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;

import logisticspipes.api.ILPPipeConfigTool;

public interface ILPPipeConfigToolWrapper {

	ILPPipeConfigTool getWrappedTool(@Nonnull ItemStack stack);
}
