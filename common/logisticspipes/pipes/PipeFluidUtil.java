/*
 * Copyright (c) 2021  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2021  RS485
 *
 * This MIT license was reworded to only match this file. If you use the regular
 * MIT license in your project, replace this copyright notice (this line and any
 * lines below and NOT the copyright line above) with the lines from the original
 * MIT license located here: http://opensource.org/licenses/MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this file and associated documentation files (the "Source Code"), to deal in
 * the Source Code without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Source Code, and to permit persons to whom the Software is furnished to
 * do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Source Code, which also can be
 * distributed under the MIT.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package logisticspipes.pipes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.ISpecialTankAccessHandler;
import logisticspipes.interfaces.ITankUtil;
import logisticspipes.pipes.basic.fluid.FluidRoutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.SpecialTankUtil;
import logisticspipes.utils.TankUtil;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.connection.NeighborTileEntity;

/**
 * Finding and reading the fluid inventories next to a fluid pipe.
 *
 * <p>Was Kotlin, where the three pipe-facing methods were extension functions on
 * {@link FluidRoutedPipe} -- which is why Java had to call them as
 * {@code PipeFluidUtil.getAdjacentTanks(pipe, flag)}. Every caller but two was already
 * Java writing it that way, so as plain statics the call sites read the same and one less object
 * exists at runtime.</p>
 */
public final class PipeFluidUtil {

	private PipeFluidUtil() {
	}

	/**
	 * The fluid inventory a block entity offers on one side, or null if it has none.
	 *
	 * <p>Storage networks come first: they hold fluids without tanks to enumerate, so they cannot be
	 * reached through the fluid capability at all. Same precedence as the item side, where
	 * {@code InventoryUtilFactory} consults the special handlers before the item capability.</p>
	 */
	@Nullable
	public static ITankUtil getTankUtilForTE(@Nullable BlockEntity tile, @Nullable Direction dirOnEntity) {
		ITankUtil special = SimpleServiceLocator.specialTankHandler.getSpecialTankUtilFor(tile, dirOnEntity);
		if (special != null) {
			return special;
		}

		if (tile == null || tile.getLevel() == null) {
			return null;
		}
		ResourceHandler<FluidResource> fluidHandler =
				tile.getLevel().getCapability(Capabilities.Fluid.BLOCK, tile.getBlockPos(), dirOnEntity);
		if (fluidHandler == null) {
			return null;
		}

		if (SimpleServiceLocator.specialTankHandler.hasHandlerFor(tile)
				&& SimpleServiceLocator.specialTankHandler.getTankHandlerFor(tile) instanceof ISpecialTankAccessHandler handler) {
			return new SpecialTankUtil(fluidHandler, tile, handler);
		}
		return new TankUtil(fluidHandler);
	}

	/** The neighbours this pipe may exchange fluid with, paired with their tank view. */
	public static List<Pair<NeighborTileEntity<BlockEntity>, ITankUtil>> getAdjacentTanks(FluidRoutedPipe pipe,
			boolean listNearbyPipes) {
		List<Pair<NeighborTileEntity<BlockEntity>, ITankUtil>> result = new ArrayList<>();
		for (NeighborTileEntity<BlockEntity> adjacent : pipe.getAvailableAdjacent().fluidTanks()) {
			if (!pipe.isConnectableTank(adjacent.getTileEntity(), adjacent.getDirection(), listNearbyPipes)) {
				continue;
			}
			ITankUtil util = getTankUtilForTE(adjacent.getTileEntity(), adjacent.getOurDirection());
			if (util != null) {
				result.add(new Pair<>(adjacent, util));
			}
		}
		return result;
	}

	public static List<BlockEntity> getAllTankTiles(FluidRoutedPipe pipe) {
		List<BlockEntity> result = new ArrayList<>();
		for (Pair<NeighborTileEntity<BlockEntity>, ITankUtil> pair : getAdjacentTanks(pipe, false)) {
			result.addAll(SimpleServiceLocator.specialTankHandler.getBaseTileFor(pair.getValue1().getTileEntity()));
		}
		return result;
	}

	/**
	 * Everything the adjacent tanks hold, as the container items a satellite advertises, with the
	 * amounts of one fluid summed into a single entry.
	 */
	public static List<ItemIdentifierStack> fluidsToItemList(PipeFluidSatellite pipe) {
		Set<FluidIdentifier> seen = new HashSet<>();
		List<ItemIdentifierStack> outputList = new ArrayList<>();
		for (Pair<NeighborTileEntity<BlockEntity>, ITankUtil> pair : getAdjacentTanks(pipe, false)) {
			for (FluidStack stack : pair.getValue2().tanks().toList()) {
				FluidIdentifierStack identStack = FluidIdentifierStack.getFromStack(stack);
				if (identStack == null) {
					continue;
				}
				ItemIdentifier container = identStack.getFluid().getItemIdentifier();
				if (seen.add(identStack.getFluid())) {
					outputList.add(container.makeStack(identStack.getAmount()));
				} else {
					// Same fluid from a second tank: fold it into the entry already listed.
					ItemIdentifierStack existing = outputList.stream()
							.filter(entry -> entry.getItem() == container)
							.findFirst()
							.orElseThrow();
					existing.setStackSize(existing.getStackSize() + identStack.getAmount());
				}
			}
		}
		return outputList;
	}
}
