package logisticspipes.interfaces;

import net.minecraft.core.Direction;

import org.jspecify.annotations.Nullable;

import logisticspipes.api.connection.Adjacent;
import logisticspipes.api.provider.IRoutedPowerProvider;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.basic.debug.DebugLogController;
import logisticspipes.request.order.ItemOrderManager;
import logisticspipes.routing.item.IRoutedItem;
import logisticspipes.utils.CacheHolder;
import logisticspipes.utils.item.ItemIdentifier;

/**
 * What modules need from the pipe they sit in, which any routed pipe can offer.
 */
public interface IPipeServiceProvider extends IRoutedPowerProvider, ISpawnParticles, ISendRoutedItem {

    boolean isNthTick(int n);

    DebugLogController getDebug();

    CacheHolder getCacheHolder();

    void markBlockEntityDirty();

    /**
     * @return the available adjacent cache.
     */
    Adjacent getAvailableAdjacent();

    /**
     * Only makes sense to use this on the chassis pipe.
     */
    @Nullable Direction getPointedOrientation();

    /**
     * To interact and send items you need to know about orders, upgrades, and have the ability to send.
     */
    ItemOrderManager getItemOrderManager();

    /**
     * Queues the item to be sent out of the pipe.
     *
     * @param from the side the item comes from
     */
    void queueRoutedItem(IRoutedItem routedItem, Direction from);

    /**
     * The upgrades that apply to a module at the given position in the pipe. Only a chassis has
     * more than one set: any other pipe answers with its own, whatever the position.
     */
    ISlotUpgradeManager getUpgradeManager(LogisticsModule.@Nullable ModulePositionType positionType, int position);

    int countOnRoute(ItemIdentifier item);
}
