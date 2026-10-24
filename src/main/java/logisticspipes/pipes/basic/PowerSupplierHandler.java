package logisticspipes.pipes.basic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import org.jspecify.annotations.Nullable;

import logisticspipes.connection.LPNeighborBlockEntity;
import logisticspipes.interfaces.ISubSystemPowerProvider;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.utils.tuples.Pair;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;
import logisticspipes.world.level.block.entity.LogisticsPowerProviderBlockEntity;

/**
 * The FE power supplier upgrade: keeps a buffer topped up from the FE power providers on the
 * network and hands it out to the blocks next to the pipe.
 */
public class PowerSupplierHandler implements ValueIOSerializable {

    private static final int BUFFER_CAPACITY = 10000;

    private final CoreRoutedPipe pipe;

    private final SimpleEnergyHandler buffer = new SimpleEnergyHandler(BUFFER_CAPACITY);

    public PowerSupplierHandler(CoreRoutedPipe pipe) {
        this.pipe = pipe;
    }

    public void serialize(ValueOutput output) {
        if (buffer.getAmountAsLong() > 0) {
            output.putDouble("bufferFE", buffer.getAmountAsLong());
        }
    }

    public void deserialize(ValueInput input) {
        // Saved as bufferRF before the RF names were dropped.
        double saved = input.getDoubleOr("bufferFE", input.getDoubleOr("bufferRF", 0.0));
        buffer.set((int) Math.clamp(saved, 0, BUFFER_CAPACITY));
    }

    public void update() {
        if (pipe.getUpgradeManager().hasFEPowerSupplierUpgrade()) {
            supplyNeighbors();
            refillBuffer();
        }
    }

    /**
     * Hands the buffer out to the neighbors. When it cannot cover them all, each gets a share in
     * proportion to what it would take.
     */
    private void supplyNeighbors() {
        int available = buffer.getAmountAsInt();
        final LogisticsGenericPipeBlockEntity container = pipe.getContainer();
        if (available == 0 || container == null) {
            return;
        }
        List<Receiver> receivers = new ArrayList<>();
        long totalDemand = 0;
        for (var adjacent : LPNeighborBlockEntity.allAround(container)) {
            if (!pipe.canPipeConnect(adjacent.getBlockEntity(), adjacent.getDirection())) {
                continue;
            }
            EnergyHandler handler = getEnergyHandler(adjacent.getBlockEntity(), adjacent.getOurDirection());
            if (handler == null) {
                continue;
            }
            // Asking the neighbor covers its free space and its insertion rate in one go, and
            // leaves out the ones that will not take energy at all, like a generator's output.
            int demand = simulateInsert(handler, available);
            if (demand > 0) {
                receivers.add(new Receiver(adjacent.getDirection(), handler, demand));
                totalDemand += demand;
            }
        }

        double fraction = Math.min(1, (double) available / totalDemand);
        for (Receiver receiver : receivers) {
            int moved = EnergyHandlerUtil.move(buffer, receiver.handler(), (int) (receiver.demand() * fraction), null);
            if (moved > 0) {
                container.addLaser(receiver.direction(), 0.5F, LogisticsPowerProviderBlockEntity.FE_COLOR, false, true);
            }
        }
    }

    /**
     * Orders what the buffer is missing from the FE providers the router can reach, split by how
     * much each one holds. The energy arrives later, through {@link #addFEPower}.
     */
    private void refillBuffer() {
        long missing = buffer.getCapacityAsLong() - buffer.getAmountAsLong();
        if (missing <= 0) {
            return;
        }
        List<ISubSystemPowerProvider> providers = new ArrayList<>();
        long stored = 0;
        for (Pair<ISubSystemPowerProvider, List<IFilter>> pair : pipe.getRouter().getSubSystemPowerProvider()) {
            ISubSystemPowerProvider provider = pair.getValue1();
            if (pair.getValue2().stream().noneMatch(IFilter::blockPower) && !provider.usePaused()) {
                providers.add(provider);
                stored += provider.getPowerLevel();
            }
        }
        if (stored > 0 && pipe.useEnergy((int) (missing / 100), false)) {
            for (ISubSystemPowerProvider provider : providers) {
                provider.requestPower(pipe.getRouterId(), (int) (missing * provider.getPowerLevel() / stored));
            }
        }
    }

    public void addFEPower(int toSend) {
        buffer.set((int) Math.min(BUFFER_CAPACITY, buffer.getAmountAsLong() + toSend));
    }

    private static @Nullable EnergyHandler getEnergyHandler(BlockEntity blockEntity, Direction face) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return null;
        }
        return level.getCapability(Capabilities.Energy.BLOCK, blockEntity.getBlockPos(), blockEntity.getBlockState(),
            blockEntity, face);
    }

    /**
     * How much of {@code amount} {@code handler} would take right now. Nothing is committed.
     */
    private static int simulateInsert(EnergyHandler handler, int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            return handler.insert(amount, transaction);
        }
    }

    private record Receiver(Direction direction, EnergyHandler handler, int demand) {}
}
