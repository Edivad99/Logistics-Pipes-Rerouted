package logisticspipes.blocks.powertile;

import javax.annotation.Nullable;

import logisticspipes.LPConfigs;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.interfaces.ICoFHEnergyStorage;
import logisticspipes.world.level.block.entity.LPBlockEntityTypes;
import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class LogisticsRFPowerProviderTileEntity extends LogisticsPowerProviderTileEntity {

	public static final int MAX_STORAGE = 10000000;
	public static final int MAX_MAXMODE = 8;
	public static final int MAX_PROVIDE_PER_TICK = 10000; //TODO

	@Getter
    private IEnergyStorage energyInterface = new IEnergyStorage() {

		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			return storage.receiveEnergy(maxReceive, simulate);
		}

		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			return 0;
		}

		@Override
		public int getEnergyStored() {
			return storage.getEnergyStored();
		}

		@Override
		public int getMaxEnergyStored() {
			return storage.getMaxEnergyStored();
		}

		@Override
		public boolean canExtract() {
			return false;
		}

		@Override
		public boolean canReceive() {
			return true;
		}
	};

	private ICoFHEnergyStorage storage;

	public LogisticsRFPowerProviderTileEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		super(LPBlockEntityTypes.BE_POWER_PROVIDER_RF.get(), pos, state);
		storage = SimpleServiceLocator.powerProxy.getEnergyStorage(10000);
	}

	public void addEnergy(double amount) {
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
		int available = (storage.extractEnergy(space, true));
		if (available > 0) {
			if (storage.extractEnergy(available, false) == available) {
				addEnergy(available);
			}
		}
	}

	private void pullFromAdjacentStorage() {
		net.minecraft.world.level.Level world = getWorld();
		if (world == null) return;
		int remaining = freeSpace();
		for (Direction dir : Direction.values()) {
			remaining = pullFromNeighbor(world, dir, remaining);
			if (remaining <= 0) return;
		}
	}

	private int pullFromNeighbor(net.minecraft.world.level.Level world, Direction dir, int remaining) {
		BlockEntity neighbor = world.getBlockEntity(getBlockPos().relative(dir));
		if (neighbor == null) return remaining;
		IEnergyStorage neighborStorage = world.getCapability(Capabilities.EnergyStorage.BLOCK, neighbor.getBlockPos(), dir.getOpposite());
		if (neighborStorage == null) return remaining;
		if (!neighborStorage.canExtract()) return remaining;
		int extracted = neighborStorage.extractEnergy(remaining, false);
		if (extracted > 0) {
			addEnergy(extracted);
			return remaining - extracted;
		}
		return remaining;
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
		maxMode = Math.min(LogisticsRFPowerProviderTileEntity.MAX_MAXMODE, Math.max(1, maxMode));
		return (LogisticsRFPowerProviderTileEntity.MAX_STORAGE / maxMode);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		storage.readFromNBT(tag);
	}

	@Override
	public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		storage.writeToNBT(tag);
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
		pipe.handleRFPowerArival(toSend);
	}

	@Override
	protected int getLaserColor() {
		return LogisticsPowerProviderTileEntity.RF_COLOR;
	}

	@Nullable
	public IEnergyStorage getEnergyStorageCap(@Nullable Direction side) {
		return energyInterface;
	}
}
