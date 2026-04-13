package logisticspipes.proxy.buildcraft;
// TODO: BuildCraft not ported to 1.20.1 — stub

import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;

import logisticspipes.api.ILPPipeConfigTool;
import logisticspipes.proxy.interfaces.ILPPipeConfigToolWrapper;

public class BuildCraftToolWrench implements ILPPipeConfigToolWrapper {
    @Override public ILPPipeConfigTool getWrappedTool(@Nonnull ItemStack stack) { return null; }
}
