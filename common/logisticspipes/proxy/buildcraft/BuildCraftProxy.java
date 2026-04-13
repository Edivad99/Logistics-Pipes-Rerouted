package logisticspipes.proxy.buildcraft;
// TODO: BuildCraft not ported to 1.20.1 — stub

import javax.annotation.Nonnull;

import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.blocks.powertile.LogisticsPowerJunctionTileEntity;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.buildcraft.subproxies.IBCPipeCapabilityProvider;
import logisticspipes.proxy.interfaces.IBCProxy;
import logisticspipes.proxy.interfaces.ICraftingRecipeProvider;
import logisticspipes.recipes.CraftingParts;

public class BuildCraftProxy implements IBCProxy {
    @Override public void registerPipeInformationProvider() {}
    @Override public void initProxy() {}
    @Override public boolean isActive() { return false; }
    @Override public boolean isInstalled() { return false; }
    @Override public CraftingParts getRecipeParts() { return null; }
    @Override public void addCraftingRecipes(CraftingParts parts) {}
    @Override public Class<? extends ICraftingRecipeProvider> getAssemblyTableProviderClass() { return null; }
    @Override public void registerInventoryHandler() {}
    @Override public IBCPipeCapabilityProvider getIBCPipeCapabilityProvider(LogisticsTileGenericPipe pipe) { return null; }
    @Override public Object createMjReceiver(@Nonnull LogisticsPowerJunctionTileEntity te) { return null; }
    @Override public boolean isBuildCraftPipe(BlockEntity tile) { return false; }
}
