package logisticspipes.proxy.interfaces;

import javax.annotation.Nullable;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.item.ItemIdentifier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface IProxy {

	String getSide();

	/**
     * Returns the client-side main level, or null on the dedicated server.
     */
	@Nullable Level getWorld();

    Player getClientPlayer();

	void addLogisticsPipesOverride(Object par1IIconRegister, int index, String override1, String override2, boolean flag);

    String getName(ItemIdentifier item);

	void updateNames(ItemIdentifier item, String name);

    void sendNameUpdateRequest(Player player);

	@Nullable LogisticsTileGenericPipe getPipeInDimensionAt(ResourceLocation dimension, int x, int y, int z, Player player);

	void sendBroadCast(String message);

	void tickServer();

	void tickClient();

    LogisticsModule getModuleFromGui();

	boolean checkSinglePlayerOwner(String commandSenderName);

	void openFluidSelectGui(int slotId);
}
