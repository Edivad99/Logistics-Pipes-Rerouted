package logisticspipes.proxy.interfaces;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;

import logisticspipes.recipes.CraftingParts;

public interface IIC2Proxy {

	void addCraftingRecipes(CraftingParts parts);

	boolean hasIC2();

	void registerToEneryNet(BlockEntity tile);

	void unregisterToEneryNet(BlockEntity tile);

	boolean acceptsEnergyFrom(BlockEntity energy, BlockEntity tile, Direction opposite);

	boolean isEnergySink(BlockEntity tile);

	double demandedEnergyUnits(BlockEntity tile);

	double injectEnergyUnits(BlockEntity tile, Direction opposite, double d);

}
