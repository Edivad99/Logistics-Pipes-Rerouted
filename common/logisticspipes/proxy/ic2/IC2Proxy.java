package logisticspipes.proxy.ic2;
// TODO: IC2 not ported to 1.20.1 — stub
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import logisticspipes.proxy.interfaces.IIC2Proxy;
import logisticspipes.recipes.CraftingParts;
public class IC2Proxy implements IIC2Proxy {
    @Override public void addCraftingRecipes(CraftingParts parts) {}
    @Override public boolean hasIC2() { return false; }
    @Override public void registerToEneryNet(BlockEntity tile) {}
    @Override public void unregisterToEneryNet(BlockEntity tile) {}
    @Override public boolean acceptsEnergyFrom(BlockEntity energy, BlockEntity tile, Direction opposite) { return false; }
    @Override public boolean isEnergySink(BlockEntity tile) { return false; }
    @Override public double demandedEnergyUnits(BlockEntity tile) { return 0; }
    @Override public double injectEnergyUnits(BlockEntity tile, Direction opposite, double d) { return 0; }
}
