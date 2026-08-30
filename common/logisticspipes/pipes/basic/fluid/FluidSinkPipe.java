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
 * of the Source Code, and to permit persons to whom the Source Code is furnished
 * to do so, subject to the following conditions:
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

package logisticspipes.pipes.basic.fluid;

import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.ITankUtil;
import logisticspipes.interfaces.routing.IFluidSink;
import logisticspipes.pipes.PipeFluidUtil;
import logisticspipes.transport.PipeFluidTransportLogistics;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.FluidSinkReply;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.connection.NeighborTileEntity;
import network.rs485.logisticspipes.property.ItemIdentifierInventoryProperty;
import network.rs485.logisticspipes.property.Property;
import network.rs485.logisticspipes.property.PropertyHolder;

public abstract class FluidSinkPipe extends FluidRoutedPipe implements IFluidSink, PropertyHolder, ValueIOSerializable {

    private final PlayerCollectionList guiOpenedBy = new PlayerCollectionList();

    private final ItemIdentifierInventoryProperty sinkInv;

    private final List<Property<?>> properties;

    public FluidSinkPipe(Item item, String inventoryName, int inventorySize) {
        super(item);
        sinkInv = new ItemIdentifierInventoryProperty(
                new ItemIdentifierInventory(inventorySize, inventoryName, 1, true), "sinkInv");
        properties = List.of(sinkInv);
    }

    public ItemIdentifierInventoryProperty getSinkInv() {
        return sinkInv;
    }

    @Override
    public List<Property<?>> getProperties() {
        return properties;
    }

    public abstract FluidSinkReply.FixedFluidPriority getPriority();

    @Override
    public @Nullable FluidSinkReply sinkAmount(FluidIdentifierStack stack) {
        if (!guiOpenedBy.isEmpty()) {
            return null; // don't sink when the gui is open
        }

        for (int i = 0; i < sinkInv.getSize(); i++) {
            ItemIdentifierStack identStack = sinkInv.getIDStackInSlot(i);
            if (identStack == null) {
                continue;
            }
            if (!stack.getFluid().equals(FluidIdentifier.get(identStack.getItem()))) {
                continue;
            }
            final int onTheWay = countOnRoute(stack.getFluid());
            long freeSpace = -onTheWay;
            for (Pair<NeighborTileEntity<BlockEntity>, ITankUtil> pair : PipeFluidUtil.getAdjacentTanks(this, true)) {
                Direction dir = pair.component1().getDirection();
                ResourceHandler<FluidResource> tank =
                        ((PipeFluidTransportLogistics) transport).getFluidResourceHandler(dir);
                freeSpace += pair.component2().getFreeSpaceInsideTank(stack.getFluid());
                freeSpace += stack.getFluid().getFreeSpaceInsideTank(tank);
                if (freeSpace >= stack.getAmount()) {
                    return new FluidSinkReply(getPriority(), stack.getAmount());
                }
            }
            return new FluidSinkReply(getPriority(), freeSpace);
        }
        return null;
    }

    public void guiOpenedByPlayer(Player player) {
        guiOpenedBy.add(player);
    }

    public void guiClosedByPlayer(Player player) {
        guiOpenedBy.remove(player);
    }

    @Override
    public boolean canInsertFromSideToTanks() {
        return true;
    }

    @Override
    public boolean canInsertToTanks() {
        return true;
    }

    @Override
    public boolean canReceiveFluid() {
        return false;
    }
}
