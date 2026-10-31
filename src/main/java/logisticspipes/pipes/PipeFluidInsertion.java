package logisticspipes.pipes;

import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.fluids.FluidStack;

import logisticspipes.pipes.basic.fluid.FluidRoutedPipe;
import logisticspipes.routing.item.IRoutedItem;
import logisticspipes.routing.item.IRoutedItem.TransportMode;
import logisticspipes.routing.sink.FluidSinkFinder;
import logisticspipes.textures.PipeTexture;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemServer;
import logisticspipes.transport.PipeFluidTransportLogistics;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.world.item.LogisticsFluidContainer;

public class PipeFluidInsertion extends FluidRoutedPipe {

    private int[] nextSendMax = new int[Direction.values().length];
    private int[] nextSendMin = new int[Direction.values().length];

    public PipeFluidInsertion(Item item) {
        super(item);
    }

    @Override
    public void enabledUpdateEntity() {
        super.enabledUpdateEntity();
        PipeFluidTransportLogistics transport = (PipeFluidTransportLogistics) this.transport;
        for (Direction dir : Direction.values()) {
            FluidStack stack = transport.sideTanks[dir.ordinal()].getFluid();
            if (stack.isEmpty()) {
                continue;
            }
            stack = stack.copy();

            if (nextSendMax[dir.ordinal()] > 0
                && stack.getAmount() < transport.sideTanks[dir.ordinal()].getCapacity()) {
                nextSendMax[dir.ordinal()]--;
                continue;
            }
            if (nextSendMin[dir.ordinal()] > 0) {
                nextSendMin[dir.ordinal()]--;
                continue;
            }

            FluidSinkFinder.BestReply result = FluidSinkFinder.getBestReply(
                FluidIdentifierStack.getFromStack(stack), getRouter(), List.of());
            if (result == null || result.reply().sinkAmount() <= 0) {
                nextSendMax[dir.ordinal()] = 100;
                nextSendMin[dir.ordinal()] = 10;
                continue;
            }

            if (!useEnergy((int) (0.01 * result.reply().getSinkAmountInt()))) {
                nextSendMax[dir.ordinal()] = 100;
                nextSendMin[dir.ordinal()] = 10;
                continue;
            }

            FluidStack toSend = transport.sideTanks[dir.ordinal()].drain(result.reply().getSinkAmountInt(), true);
            ItemIdentifierStack liquidContainer = LogisticsFluidContainer.of(
                FluidIdentifierStack.getFromStack(toSend));
            IRoutedItem routed = new LPTravelingItemServer(liquidContainer);
            routed.setDestination(result.routerId());
            routed.setTransportMode(TransportMode.Passive);
            this.queueRoutedItem(routed, dir);
            nextSendMax[dir.ordinal()] = 100;
            nextSendMin[dir.ordinal()] = 5;
        }
    }

    @Override
    public void serialize(ValueOutput output) {
        super.serialize(output);
        output.putIntArray("nextSendMax", nextSendMax);
        output.putIntArray("nextSendMin", nextSendMin);
    }

    @Override
    public void deserialize(ValueInput input) {
        super.deserialize(input);
        nextSendMax = input.getIntArray("nextSendMax").orElse(new int[0]);
        if (nextSendMax.length < 6) {
            nextSendMax = new int[6];
        }
        nextSendMin = input.getIntArray("nextSendMin").orElse(new int[0]);
        if (nextSendMin.length < 6) {
            nextSendMin = new int[6];
        }
    }

    @Override
    public PipeTexture getCenterTexture() {
        return PipeTexture.LIQUID_INSERTION;
    }

    @Override
    public boolean canInsertToTanks() {
        return false;
    }

    @Override
    public boolean canInsertFromSideToTanks() {
        return false;
    }

    @Override
    public boolean canReceiveFluid() {
        return true;
    }
}
