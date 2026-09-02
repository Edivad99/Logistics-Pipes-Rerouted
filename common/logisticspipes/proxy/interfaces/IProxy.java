package logisticspipes.proxy.interfaces;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import org.jspecify.annotations.Nullable;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

public interface IProxy {

	String getSide();

    void addLogisticsPipesOverride(Object par1IIconRegister, int index, String override1, String override2, boolean flag);

	@Nullable LogisticsTileGenericPipe getPipeInDimensionAt(Identifier dimension, int x, int y, int z, Player player);

	void sendBroadCast(String message);

	void tickServer();

	void tickClient();

    @Nullable LogisticsModule getModuleFromGui();

	boolean checkSinglePlayerOwner(String commandSenderName);

	void openFluidSelectGui(int slotId);
}
