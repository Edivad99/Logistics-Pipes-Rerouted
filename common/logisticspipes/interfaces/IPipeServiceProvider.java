package logisticspipes.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.jspecify.annotations.Nullable;

import logisticspipes.api.IRoutedPowerProvider;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.basic.debug.DebugLogController;
import logisticspipes.routing.order.LogisticsItemOrderManager;
import logisticspipes.utils.CacheHolder;
import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.connection.Adjacent;

//methods needed by modules that any CRP can offer
public interface IPipeServiceProvider extends IRoutedPowerProvider, ISpawnParticles, ISendRoutedItem {

	boolean isNthTick(int n);

	DebugLogController getDebug();

	CacheHolder getCacheHolder();

	@Nullable BlockPos getPos();

	void markTileDirty();

	/**
	 * @return the available adjacent cache.
	 */
    Adjacent getAvailableAdjacent();

	/**
	 * Only makes sense to use this on the chassis pipe.
	 */
	@Nullable
	Direction getPointedOrientation();

	/**
	 * to interact and send items you need to know about orders, upgrades, and have the ability to send
	 */
	LogisticsItemOrderManager getItemOrderManager();

	void queueRoutedItem(IRoutedItem routedItem, Direction from);

	ISlotUpgradeManager getUpgradeManager(LogisticsModule.ModulePositionType slot, int positionInt);

	int countOnRoute(ItemIdentifier item);
}
