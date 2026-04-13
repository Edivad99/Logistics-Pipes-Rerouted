package logisticspipes.proxy.cc;
// TODO: ComputerCraft not ported to 1.20.1 — stub

import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.interfaces.ICCProxy;
import logisticspipes.recipes.CraftingParts;

public class CCProxy implements ICCProxy {
    @Override public boolean isTurtle(BlockEntity tile) { return false; }
    @Override public boolean isComputer(BlockEntity tile) { return false; }
    @Override public boolean isCC() { return false; }
    @Override public boolean isLuaThread(Thread thread) { return false; }
    @Override public void queueEvent(String event, Object[] arguments, LogisticsTileGenericPipe pipe) {}
    @Override public void setTurtleConnect(boolean flag, LogisticsTileGenericPipe pipe) {}
    @Override public boolean getTurtleConnect(LogisticsTileGenericPipe pipe) { return false; }
    @Override public int getLastCCID(LogisticsTileGenericPipe pipe) { return 0; }
    @Override public void handleMesssage(int computerId, Object message, LogisticsTileGenericPipe tile, int sourceId) {}
    @Override public void addCraftingRecipes(CraftingParts parts) {}
    @Override public Object getAnswer(Object object) { return null; }
}
