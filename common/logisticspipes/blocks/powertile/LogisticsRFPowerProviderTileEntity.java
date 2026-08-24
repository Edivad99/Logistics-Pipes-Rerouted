package logisticspipes.blocks.powertile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.DelegatingEnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import org.jspecify.annotations.Nullable;

import logisticspipes.LPConfigs;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.world.level.block.entity.LPBlockEntityTypes;

public class LogisticsRFPowerProviderTileEntity extends LogisticsPowerProviderTileEntity {

	public static final int MAX_STORAGE = 10000000;
	public static final int MAX_MAXMODE = 8;
	public static final int MAX_PROVIDE_PER_TICK = 10000; //TODO

	/**
	 * The RF buffer this provider fills from its neighbours and drains into the LP network.
	 *
	 * <p>Was LP's own {@code ICoFHEnergyStorage} over NeoForge's {@code EnergyStorage}, both now
	 * removed with the 21.9 transfer rework. {@link SimpleEnergyHandler} is the drop-in: same
	 * capacity/insert/extract model, already transactional, and already a
	 * {@code ValueIOSerializable} -- which is every method the old LP interface declared.</p>
	 *
	 * <p>Exposed to neighbours through {@link #energyInterface}, which is the same buffer with
	 * extraction closed off: LP hands its power to the network itself rather than letting anything
	 * pull it back out.</p>
	 */
	private final SimpleEnergyHandler storage = new SimpleEnergyHandler(10000);

	private final EnergyHandler energyInterface = new DelegatingEnergyHandler(() -> storage) {
		@Override
		public int extract(int amount, TransactionContext transaction) {
			return 0;
		}
	};

	public LogisticsRFPowerProviderTileEntity(BlockPos pos, BlockState state) {
		super(LPBlockEntityTypes.POWER_PROVIDER_RF.get(), pos, state);
	}

	private void addEnergy(double amount) {
		if (MainProxy.isClient(getWorld())) {
			return;
		}
		internalStorage += amount;
		if (internalStorage > LogisticsRFPowerProviderTileEntity.MAX_STORAGE) {
			internalStorage = LogisticsRFPowerProviderTileEntity.MAX_STORAGE;
		}
		if (internalStorage >= getMaxStorage()) {
			needMorePowerTriggerCheck = false;
		}
	}

	private void addStoredRF() {
		int space = freeSpace();
		// One transaction instead of the old simulate-then-execute pair: extract, and keep it only
		// if the buffer really gave up what it offered.
		try (Transaction transaction = Transaction.openRoot()) {
			int available = storage.extract(space, transaction);
			if (available > 0) {
				addEnergy(available);
				transaction.commit();
			}
		}
	}

	private void pullFromAdjacentStorage() {
		if (this.level == null) {
            return;
        }
		int remaining = freeSpace();
		for (Direction dir : Direction.values()) {
			remaining = pullFromNeighbor(this.level, dir, remaining);
			if (remaining <= 0) {
                return;
            }
		}
	}

	private int pullFromNeighbor(Level level, Direction dir, int remaining) {
		BlockEntity neighbor = level.getBlockEntity(getBlockPos().relative(dir));
		if (neighbor == null) {
            return remaining;
        }
		EnergyHandler neighborStorage = level.getCapability(Capabilities.Energy.BLOCK, neighbor.getBlockPos(), dir.getOpposite());
		if (neighborStorage == null) {
            return remaining;
        }
		try (Transaction transaction = Transaction.openRoot()) {
			int extracted = neighborStorage.extract(remaining, transaction);
			if (extracted <= 0) {
				return remaining;
			}
			addEnergy(extracted);
			transaction.commit();
			return remaining - extracted;
		}
	}

	public int freeSpace() {
		return (int) (getMaxStorage() - internalStorage);
	}

	@Override
	public void update() {
		super.update();
		if (MainProxy.isServer(getWorld())) {
			if (freeSpace() > 0) {
				if (LPConfigs.COMMON.POWER_SOURCE_MODE.get().equals(LPConfigs.PowerSourceMode.ADJACENT)) {
					pullFromAdjacentStorage();
				} else {
					addStoredRF();
				}
			}
		}
	}

	@Override
	public int getMaxStorage() {
		maxMode = Math.clamp(maxMode, 1, LogisticsRFPowerProviderTileEntity.MAX_MAXMODE);
		return (LogisticsRFPowerProviderTileEntity.MAX_STORAGE / maxMode);
	}

	@Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
		storage.deserialize(input);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		storage.serialize(output);
	}

	@Override
	public String getBrand() {
		return "RF";
	}

	@Override
	protected double getMaxProvidePerTick() {
		return LogisticsRFPowerProviderTileEntity.MAX_PROVIDE_PER_TICK;
	}

	@Override
	protected void handlePower(CoreRoutedPipe pipe, double toSend) {
		pipe.handleRFPowerArrival(toSend);
	}

	@Override
	protected int getLaserColor() {
		return LogisticsPowerProviderTileEntity.RF_COLOR;
	}

	@Nullable
	public EnergyHandler getEnergyStorageCap(@Nullable Direction side) {
		return energyInterface;
	}
}
