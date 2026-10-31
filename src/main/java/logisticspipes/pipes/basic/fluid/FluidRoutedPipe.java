package logisticspipes.pipes.basic.fluid;

import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

import org.jspecify.annotations.Nullable;

import logisticspipes.LogisticsPipes;
import logisticspipes.api.util.ITankUtil;
import logisticspipes.interfaces.routing.IRequireReliableFluidTransport;
import logisticspipes.inventory.tank.SpecialTankHandler;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.PipeFluidUtil;
import logisticspipes.pipes.PipeFluidUtil.AdjacentTank;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.request.order.FluidOrderManager;
import logisticspipes.request.order.OrderManager;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.RouterManager;
import logisticspipes.routing.item.IRoutedItem;
import logisticspipes.routing.item.IRoutedItem.TransportMode;
import logisticspipes.routing.item.ItemRoutingInformation;
import logisticspipes.routing.sink.FluidSinkFinder;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemServer;
import logisticspipes.transport.PipeFluidTransportLogistics;
import logisticspipes.utils.CacheHolder.CacheTypes;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.transfer.LPFluidTank;
import logisticspipes.world.item.LogisticsFluidContainer;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;

public abstract class FluidRoutedPipe extends CoreRoutedPipe {

    private @Nullable FluidOrderManager orderFluidManager;

    public FluidRoutedPipe(Item item) {
        super(new PipeFluidTransportLogistics(), item);
    }

    @Override
    public void setTile(BlockEntity blockEntity) {
        super.setTile(blockEntity);
    }

    @Override
    public boolean logisticsIsPipeConnected(BlockEntity blockEntity, Direction dir) {
        ITankUtil tank = PipeFluidUtil.getTankUtilForBE(blockEntity, dir.getOpposite());
        return (tank != null && tank.containsTanks()) || blockEntity instanceof LogisticsGenericPipeBlockEntity;
    }

    @Override
    public ItemSendMode getItemSendMode() {
        return ItemSendMode.NORMAL;
    }

    @Override
    public @Nullable LogisticsModule getLogisticsModule() {
        return null;
    }

    /***
     * @param blockEntity
     *            The connected BlockEntity
     * @param dir
     *            The direction the BlockEntity is in relative to the current
     *            pipe
     * @param flag
     *            Whether to list a Nearby Pipe or not
     */

    public final boolean isConnectableTank(BlockEntity blockEntity, Direction dir, boolean flag) {
        if (SpecialTankHandler.get().hasHandlerFor(blockEntity)) {
            return true;
        }
        // dir points from this pipe to the blockEntity, so the blockEntity is asked about the side facing us -- the
        // same side getTankUtil() uses when it builds the util we would end up filling.
        boolean fluidTile = SpecialTankHandler.get().hasSpecialTankUtilFor(blockEntity, dir.getOpposite());
        Level level = blockEntity.getLevel();
        if (level != null) {
            var fluidHandler = level.getCapability(Capabilities.Fluid.BLOCK, blockEntity.getBlockPos(), dir);
            if (fluidHandler != null) {
                fluidTile = true;
            }
        }
        if (!fluidTile) {
            return false;
        }
        if (!this.canPipeConnect(blockEntity, dir)) {
            return false;
        }
        if (blockEntity instanceof LogisticsGenericPipeBlockEntity genericPipeBlockEntity) {
            if (genericPipeBlockEntity.pipe instanceof FluidRoutedPipe) {
                return false;
            }
            if (!flag) {
                return false;
            }
            // Was `transport instanceof IFluidHandler`, which no LP transport has ever satisfied --
            // the handler was an inner class of PipeFluidTransportLogistics, never the transport
            // itself, so this always said "not a fluid pipe". Naming the transport type is what the
            // check was reaching for, and lands on the same answer for every pipe that exists.
            return genericPipeBlockEntity.pipe != null
                && genericPipeBlockEntity.pipe.transport instanceof PipeFluidTransportLogistics;
        }
        return true;
    }

    @Override
    public void enabledUpdateEntity() {
        super.enabledUpdateEntity();
        if (canInsertFromSideToTanks()) {
            int validDirections = 0;
            final List<AdjacentTank> list =
                PipeFluidUtil.getAdjacentTanks(this, true);
            for (AdjacentTank adjacent : list) {
                if (adjacent.neighbor().getBlockEntity() instanceof LogisticsGenericPipeBlockEntity pipeBlockEntity
                    && pipeBlockEntity.pipe instanceof CoreRoutedPipe) {
                    continue;
                }
                LPFluidTank internalTank = ((PipeFluidTransportLogistics) transport)
                    .sideTanks[adjacent.neighbor().getDirection().ordinal()];
                validDirections++;
                if (internalTank.getFluid().isEmpty()) {
                    continue;
                }
                int filled = adjacent.tank().fill(internalTank.getFluid(), true);
                if (filled == 0) {
                    continue;
                }
                FluidStack drain = internalTank.drain(filled, true);
                if (filled != drain.getAmount()) {
                    LogisticsPipes.LOG.warn("Fluid multiplication at {}: filled {} mB but drained {} mB",
                        getPos(), filled, drain.getAmount());
                    if (LogisticsPipes.isDEBUG()) {
                        throw new UnsupportedOperationException("Fluid Multiplication");
                    }
                }
            }
            if (validDirections == 0) {
                return;
            }
            LPFluidTank tank = ((PipeFluidTransportLogistics) transport).internalTank;
            FluidStack stack = tank.getFluid();
            if (stack.isEmpty()) {
                return;
            }
            for (AdjacentTank adjacent : list) {
                if (adjacent.neighbor().isLogisticsPipe()) {
                    if (((LogisticsGenericPipeBlockEntity) adjacent.neighbor()
                        .getBlockEntity()).pipe instanceof CoreRoutedPipe) {
                        continue;
                    }
                }
                LPFluidTank tankSide = ((PipeFluidTransportLogistics) transport)
                    .sideTanks[adjacent.neighbor().getDirection().ordinal()];
                stack = tank.getFluid();
                if (stack.isEmpty()) {
                    continue;
                }
                stack = stack.copy();
                int filled = tankSide.fill(stack, true);
                if (filled == 0) {
                    continue;
                }
                FluidStack drain = tank.drain(filled, true);
                if (filled != drain.getAmount()) {
                    LogisticsPipes.LOG.warn("Fluid multiplication at {}: filled {} mB but drained {} mB",
                        getPos(), filled, drain.getAmount());
                    if (LogisticsPipes.isDEBUG()) {
                        throw new UnsupportedOperationException("Fluid Multiplication");
                    }
                }
            }
        }
    }

    public int countOnRoute(FluidIdentifier ident) {
        int amount = 0;
        for (ItemRoutingInformation next : inTransitToMe) {
            ItemIdentifierStack item = next.getItem();
            if (item.getItem().isFluidContainer()) {
                FluidIdentifierStack liquid = LogisticsFluidContainer.getFluid(item);
                if (liquid.getFluid().equals(ident)) {
                    amount += liquid.getAmount();
                }
            }
        }
        return amount;
    }

    public abstract boolean canInsertFromSideToTanks();

    public abstract boolean canInsertToTanks();

    public abstract boolean canReceiveFluid();

    public boolean endReached(LPTravelingItemServer arrivingItem, BlockEntity tile) {
        if (canInsertToTanks() && !getLevel().isClientSide()) {
            getCacheHolder().trigger(CacheTypes.Inventory);
            if (arrivingItem.getItemIdentifierStack() == null || !(arrivingItem.getItemIdentifierStack().getItem()
                .isFluidContainer())) {
                return false;
            }
            if (getRouter().getSimpleID() != arrivingItem.getDestination()) {
                return false;
            }
            int filled;
            FluidIdentifierStack liquid = LogisticsFluidContainer.getFluid(
                arrivingItem.getItemIdentifierStack());
            if (isConnectableTank(tile, arrivingItem.output, false)) {
                //Try to put liquid into all adjacent tanks.
                for (AdjacentTank adjacent : PipeFluidUtil.getAdjacentTanks(this,
                    false)) {
                    filled = adjacent.tank().fill(liquid.makeFluidStack(), true);
                    liquid.lowerAmount(filled);
                    if (liquid.getAmount() != 0) {
                        continue;
                    }
                    return true;
                }
                //Try inserting the liquid into the pipe side tank
                filled = ((PipeFluidTransportLogistics) transport).sideTanks[arrivingItem.output.ordinal()].fill(
                    liquid.makeFluidStack(), true);
                if (filled == liquid.getAmount()) {
                    return true;
                }
                liquid.lowerAmount(filled);
            }
            //Try inserting the liquid into the pipe internal tank
            filled = ((PipeFluidTransportLogistics) transport).internalTank.fill(liquid.makeFluidStack(), true);
            if (filled == liquid.getAmount()) {
                return true;
            }
            //If liquids still exist,
            liquid.lowerAmount(filled);

            if (this instanceof IRequireReliableFluidTransport requireReliableFluidTransport) {
                requireReliableFluidTransport.liquidNotInserted(liquid.getFluid(), liquid.getAmount());
            }

            IRoutedItem routedItem = new LPTravelingItemServer(
                LogisticsFluidContainer.of(liquid));
            // Carry forward the arriving item's jam list so the rerouted remainder does
            // not immediately pick the same (now-full) path again, and add this pipe's
            // router to prevent looping back here on the very next hop.
            for (int simpleId : arrivingItem.getJamList()) {
                IRouter r = RouterManager.get().getRouter(simpleId);
                if (r != null) {
                    routedItem.addToJamList(r);
                }
            }
            routedItem.addToJamList(getRouter());
            FluidSinkFinder.BestReply replies = FluidSinkFinder.getBestReply(liquid,
                getRouter(), routedItem.getJamList());
            if (replies == null) {
                // clear destination without marking item as lost
                routedItem.setDestination(0);
            } else {
                int dest = replies.routerId();
                routedItem.setDestination(dest);
            }
            routedItem.setTransportMode(TransportMode.Passive);
            this.queueRoutedItem(routedItem, arrivingItem.output.getOpposite());
            return true;
        }
        return false;
    }

    @Override
    public boolean isFluidPipe() {
        return true;
    }

    @Override
    public boolean isOnSameContainer(CoreRoutedPipe other) {
        if (other instanceof FluidRoutedPipe fluidRoutedPipe) {
            List<BlockEntity> theirs = PipeFluidUtil.getAllTankTiles(fluidRoutedPipe);
            for (BlockEntity blockEntity : PipeFluidUtil.getAllTankTiles(this)) {
                if (theirs.contains(blockEntity)) {
                    return true;
                }
            }
        }
        return false;
    }

    public FluidOrderManager getFluidOrderManager() {
        orderFluidManager = orderFluidManager != null ? orderFluidManager : new FluidOrderManager(this);
        return orderFluidManager;
    }

    @Override
    public OrderManager<?, ?> getOrderManager() {
        return getFluidOrderManager();
    }
}
