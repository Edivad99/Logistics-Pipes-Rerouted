package logisticspipes.pipes.basic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import logisticspipes.interfaces.ISubSystemPowerProvider;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.tuples.Pair;
import logisticspipes.world.WorldCoordinatesWrapper;
import logisticspipes.world.level.block.entity.LogisticsPowerProviderBlockEntity;

public class PowerSupplierHandler implements ValueIOSerializable {

    private static final double INTERNAL_FE_BUFFER_MAX = 10000;

    private final CoreRoutedPipe pipe;

    private double internalBufferFE = 0F;

    public PowerSupplierHandler(CoreRoutedPipe pipe) {
        this.pipe = pipe;
    }

    public void serialize(ValueOutput output) {
        if (internalBufferFE > 0) {
            output.putDouble("bufferFE", internalBufferFE);
        }
    }

    public void deserialize(ValueInput input) {
        // Saved as bufferRF before the RF names were dropped.
        internalBufferFE = input.getDoubleOr("bufferFE", input.getDoubleOr("bufferRF", 0.0));
    }

    public void update() {
        if (pipe.getUpgradeManager().hasFEPowerSupplierUpgrade()) {
            requestFEPower();
        }
    }

    private void requestFEPower() {
        // Hand the buffer out to the neighbors, each in proportion to what it can still take.
        List<Receiver> receivers = new ArrayList<>();
        double globalNeed = 0;
        for (var adjacent : new WorldCoordinatesWrapper(pipe.getContainer()).allNeighborBlockEntities()) {
            if (!pipe.canPipeConnect(adjacent.getBlockEntity(), adjacent.getDirection())) {
                continue;
            }
            // Null when the neighbor has the capability but will not take energy -- a
            // generator's output buffer, say. It used to be filtered by canReceive().
            EnergyHandler handler = SimpleServiceLocator.powerProxy
                .getEnergyReceiver(adjacent.getBlockEntity(), adjacent.getOurDirection());
            if (handler != null) {
                double need = handler.getCapacityAsLong() - handler.getAmountAsLong();
                receivers.add(new Receiver(adjacent.getDirection(), handler, need));
                globalNeed += need;
            }
        }

        if (globalNeed != 0) {
            double fulfillable = Math.min(1, internalBufferFE / globalNeed);
            for (Receiver receiver : receivers) {
                double share = receiver.need() * fulfillable;
                if (internalBufferFE + 1 < share) {
                    return;
                }
                int used;
                try (Transaction transaction = Transaction.openRoot()) {
                    used = receiver.handler().insert((int) share, transaction);
                    transaction.commit();
                }
                if (used > 0) {
                    pipe.getContainer()
                        .addLaser(receiver.direction(), 0.5F, LogisticsPowerProviderBlockEntity.FE_COLOR, false, true);
                    internalBufferFE -= used;
                }
                if (internalBufferFE < 0) {
                    internalBufferFE = 0;
                    return;
                }
            }
        }

        // Top the buffer back up from the FE providers the router can reach.
        List<ISubSystemPowerProvider> providers = new ArrayList<>();
        double available = 0;
        for (Pair<ISubSystemPowerProvider, List<IFilter>> pair : pipe.getRouter().getSubSystemPowerProvider()) {
            ISubSystemPowerProvider provider = pair.getValue1();
            if (pair.getValue2().stream().noneMatch(IFilter::blockPower) && !provider.usePaused()
                && provider.getBrand().equals("FE")) {
                providers.add(provider);
                available += provider.getPowerLevel();
            }
        }
        if (available > 0) {
            double neededPower = PowerSupplierHandler.INTERNAL_FE_BUFFER_MAX - internalBufferFE;
            if (neededPower > 0 && pipe.useEnergy((int) (neededPower / 100), false)) {
                for (ISubSystemPowerProvider provider : providers) {
                    provider.requestPower(pipe.getRouterId(), neededPower * (provider.getPowerLevel() / available));
                }
            }
        }
    }

    public void addFEPower(double toSend) {
        internalBufferFE += toSend;
    }

    private record Receiver(Direction direction, EnergyHandler handler, double need) {}
}
