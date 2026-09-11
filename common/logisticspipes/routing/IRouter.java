/**
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.routing;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import org.jspecify.annotations.Nullable;

import logisticspipes.api.provider.ILogisticsPowerProvider;
import logisticspipes.interfaces.ISubSystemPowerProvider;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.tuples.Pair;

public interface IRouter {

	void destroy();

	void update(boolean doFullRefresh, CoreRoutedPipe pipe);

	boolean isRoutedExit(Direction connection);

	boolean isSubPoweredExit(Direction connection);

	int getDistanceToNextPowerPipe(Direction dir);

	boolean hasRoute(int id, boolean active, ItemIdentifier type);

	@Nullable ExitRoute getExitFor(int id, boolean active, ItemIdentifier type);

	/**
	 * The route table, indexed by router id, with a null entry for every id this router has no route
	 * to -- the list is sized by the largest id in the network, not by how many are reachable.
	 */
	List<@Nullable List<ExitRoute>> getRouteTable();

	List<ExitRoute> getIRoutersByCost();

	@Nullable CoreRoutedPipe getPipe();

	@Nullable CoreRoutedPipe getCachedPipe();

	boolean isInDim(Identifier dimension);

	boolean isAt(Identifier dimension, BlockPos pos);

	UUID getId();

	@Nullable LogisticsModule getLogisticsModule();

	void clearPipeCache();

	int getSimpleID();

	BlockPos getPos();

	/* Automated Disconnection */
	boolean isSideDisconnected(Direction dir);

	/**
	 * The routes to {@code r}, empty when there are none. Never null: a client router has no
	 * routing table at all and answers with an empty list like any other router with nothing to
	 * offer, so callers can iterate without checking.
	 */
	List<ExitRoute> getDistanceTo(IRouter r);

	void clearInterests();

	List<Pair<ILogisticsPowerProvider, List<IFilter>>> getPowerProvider();

	List<Pair<ISubSystemPowerProvider, List<IFilter>>> getSubSystemPowerProvider();

	boolean isCacheInvalid();

	//force-update LSA version in the network
	void forceLsaUpdate();

	List<ExitRoute> getRoutersOnSide(Direction direction);

	void queueTask(int i, IRouterQueuedTask callable);

}
