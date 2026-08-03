/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.utils;

import java.util.ArrayList;
import java.util.Objects;
import javax.annotation.Nullable;
import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.proxy.specialinventoryhandler.SpecialInventoryHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import network.rs485.logisticspipes.connection.NeighborTileEntity;
import network.rs485.logisticspipes.inventory.ProviderMode;

public class InventoryUtilFactory {

	private final ArrayList<SpecialInventoryHandler.Factory> handlerFactories = new ArrayList<>();

	public void registerHandler(SpecialInventoryHandler.Factory handlerFactory) {
		if (handlerFactory.init()) {
			handlerFactories.add(handlerFactory);
			LogisticsPipes.LOG.info("Loaded SpecialInventoryHandler.Factory: " + handlerFactory.getClass().getCanonicalName());
		} else {
			LogisticsPipes.LOG.warn("Could not load SpecialInventoryHandler.Factory: " + handlerFactory.getClass().getCanonicalName());
		}
	}

	@Nullable
	public SpecialInventoryHandler getSpecialHandlerFor(BlockEntity tile, Direction direction, ProviderMode mode) {
		return handlerFactories.stream()
				.filter(factory -> factory.isType(tile, direction))
				.map(factory -> factory.getUtilForTile(tile, direction, mode))
				.filter(Objects::nonNull)
				.findAny()
				.orElse(null);
	}

	@Nullable
	public IInventoryUtil getInventoryUtil(NeighborTileEntity<BlockEntity> adj) {
		return getHidingInventoryUtil(adj.getTileEntity(), adj.getOurDirection(), ProviderMode.DEFAULT);
	}

	@Nullable
	public IInventoryUtil getInventoryUtil(BlockEntity inv, Direction dir) {
		return getHidingInventoryUtil(inv, dir, ProviderMode.DEFAULT);
	}

	@Nullable
	public IInventoryUtil getHidingInventoryUtil(@Nullable BlockEntity tile, @Nullable Direction direction, ProviderMode mode) {
		if (tile != null) {
			IInventoryUtil util = getSpecialHandlerFor(tile, direction, mode);
			if (util != null) {
				return util;
			}
			// Pass the known block entity: the short getCapability() overload looks the state and the
			// block entity up on the level, and that goes through getChunk() — a *blocking chunk load*.
			// Called from the pipe adjacency scan, which also runs while a chunk is being unloaded, it
			// would start a chunk generation task the server can no longer finish and hang the save.
			IItemHandler handler = tile.getLevel().getCapability(
					Capabilities.ItemHandler.BLOCK, tile.getBlockPos(), tile.getBlockState(), tile, direction);
			if (handler != null) {
				return new InventoryUtil(handler, mode);
			}
		}
		return null;
	}
}
