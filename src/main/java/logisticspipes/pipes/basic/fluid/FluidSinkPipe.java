package logisticspipes.pipes.basic.fluid;

import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import logisticspipes.api.property.Property;
import logisticspipes.api.property.PropertyHolder;
import logisticspipes.interfaces.routing.IFluidSink;
import logisticspipes.pipes.PipeFluidUtil;
import logisticspipes.pipes.PipeFluidUtil.AdjacentTank;
import logisticspipes.property.ItemIdentifierInventoryProperty;
import logisticspipes.transport.PipeFluidTransportLogistics;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.FluidSinkReply;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;

public abstract class FluidSinkPipe extends FluidRoutedPipe implements IFluidSink, PropertyHolder, ValueIOSerializable {

    private final PlayerCollectionList guiOpenedBy = new PlayerCollectionList();

    @Getter
    private final ItemIdentifierInventoryProperty sinkInv;

    private final List<Property<?>> properties;

    public FluidSinkPipe(Item item, String inventoryName, int inventorySize) {
        super(item);
        sinkInv = new ItemIdentifierInventoryProperty(
                new ItemIdentifierInventory(inventorySize, inventoryName, 1, true), "sinkInv");
        properties = List.of(sinkInv);
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
            for (AdjacentTank adjacent : PipeFluidUtil.getAdjacentTanks(this, true)) {
                Direction dir = adjacent.neighbor().getDirection();
                ResourceHandler<FluidResource> tank =
                        ((PipeFluidTransportLogistics) transport).getFluidResourceHandler(dir);
                freeSpace += adjacent.tank().getFreeSpaceInsideTank(stack.getFluid().asResource());
                freeSpace += stack.getFluid().getFreeSpaceInsideTank(tank);
                if (freeSpace >= stack.getAmount()) {
                    return new FluidSinkReply(getPriority(), stack.getAmount());
                }
            }
            return new FluidSinkReply(getPriority(), freeSpace);
        }
        return null;
    }

    public void screenOpenedByPlayer(Player player) {
        guiOpenedBy.add(player);
    }

    public void screenClosedByPlayer(Player player) {
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
