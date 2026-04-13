package logisticspipes.proxy.interfaces;

import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.recipes.CraftingParts;

public interface ICCProxy {

	boolean isTurtle(BlockEntity tile);

	boolean isComputer(BlockEntity tile);

	boolean isCC();

	boolean isLuaThread(Thread thread);

	void queueEvent(String event, Object[] arguments, LogisticsTileGenericPipe logisticsTileGenericPipe);

	void setTurtleConnect(boolean flag, LogisticsTileGenericPipe logisticsTileGenericPipe);

	boolean getTurtleConnect(LogisticsTileGenericPipe logisticsTileGenericPipe);

	int getLastCCID(LogisticsTileGenericPipe logisticsTileGenericPipe);

	void handleMesssage(int computerId, Object message, LogisticsTileGenericPipe tile, int sourceId);

	void addCraftingRecipes(CraftingParts parts);

	Object getAnswer(Object object);
}
