package logisticspipes.pipes.basic;

import java.util.List;
import logisticspipes.blocks.powertile.LogisticsPowerProviderTileEntity;
import logisticspipes.interfaces.ISubSystemPowerProvider;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.interfaces.ICoFHEnergyReceiver;
import logisticspipes.utils.tuples.Pair;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import network.rs485.logisticspipes.connection.LPNeighborTileEntity;
import network.rs485.logisticspipes.connection.NeighborTileEntity;
import network.rs485.logisticspipes.world.WorldCoordinatesWrapper;

public class PowerSupplierHandler {

	private static final double INTERNAL_RF_BUFFER_MAX = 10000;

	private final CoreRoutedPipe pipe;

	private double internalBufferRF = 0F;
	private double internalBufferIC2 = 0F;

	public PowerSupplierHandler(CoreRoutedPipe pipe) {
		this.pipe = pipe;
	}

	public void writeToNBT(CompoundTag nbttagcompound) {
		if (internalBufferRF > 0) {
			nbttagcompound.putDouble("bufferRF", internalBufferRF);
		}
		if (internalBufferIC2 > 0) {
			nbttagcompound.putDouble("bufferEU", internalBufferIC2);
		}
	}

	public void readFromNBT(CompoundTag nbttagcompound) {
		if (nbttagcompound.get("bufferRF") instanceof FloatTag) { // support for old float
			internalBufferRF = nbttagcompound.getFloatOr("bufferRF", 0.0f);
		} else {
			internalBufferRF = nbttagcompound.getDoubleOr("bufferRF", 0.0);
		}
		if (nbttagcompound.get("bufferEU") instanceof FloatTag) { // support for old float
			internalBufferIC2 = nbttagcompound.getFloatOr("bufferEU", 0.0f);
		} else {
			internalBufferIC2 = nbttagcompound.getDoubleOr("bufferEU", 0.0);
		}
	}

	public void update() {
		if (SimpleServiceLocator.powerProxy.isAvailable() && pipe.getUpgradeManager().hasRFPowerSupplierUpgrade()) {
			if (requestRFPower()) return;
		}
		// IC2/EU distribution removed — IC2 has no 1.20.1 port, the former dummy proxy made
		// this path a no-op (hasIC2() was always false).
	}

	private boolean requestRFPower() {
		//Use Buffer

		final List<LPNeighborTileEntity<BlockEntity>> adjacentTileEntities = new WorldCoordinatesWrapper(pipe.container).allNeighborTileEntities();

		double globalNeed = 0;
		double[] need = new double[adjacentTileEntities.size()];
		int i = 0;
		for (NeighborTileEntity<BlockEntity> adjacent : adjacentTileEntities) {
			if (SimpleServiceLocator.powerProxy.isEnergyReceiver(adjacent.getTileEntity(), adjacent.getOurDirection())) {
				if (pipe.canPipeConnect(adjacent.getTileEntity(), adjacent.getDirection())) {
					ICoFHEnergyReceiver energyReceiver = SimpleServiceLocator.powerProxy.getEnergyReceiver(adjacent.getTileEntity(), adjacent.getOurDirection());
					globalNeed += need[i] = (energyReceiver.getMaxEnergyStored() - energyReceiver.getEnergyStored());
				}
			}
			++i;
		}

		if (globalNeed != 0 && !Double.isNaN(globalNeed)) {
			double fullfillable = Math.min(1, internalBufferRF / globalNeed);
			i = 0;
			for (NeighborTileEntity<BlockEntity> adjacent : adjacentTileEntities) {
				if (SimpleServiceLocator.powerProxy.isEnergyReceiver(adjacent.getTileEntity(), adjacent.getOurDirection())) {
					if (pipe.canPipeConnect(adjacent.getTileEntity(), adjacent.getDirection())) {
						Direction oppositeDir = adjacent.getOurDirection();
						ICoFHEnergyReceiver energyReceiver = SimpleServiceLocator.powerProxy.getEnergyReceiver(adjacent.getTileEntity(), oppositeDir);
						if (internalBufferRF + 1 < need[i] * fullfillable) {
							return true;
						}
						int used = energyReceiver.receiveEnergy(oppositeDir, (int) (need[i] * fullfillable), false);
						if (used > 0) {
							pipe.container.addLaser(adjacent.getDirection(), 0.5F, LogisticsPowerProviderTileEntity.RF_COLOR, false, true);
							internalBufferRF -= used;
						}
						if (internalBufferRF < 0) {
							internalBufferRF = 0;
							return true;
						}
					}
				}
				++i;
			}
		}
		//Rerequest Buffer
		List<Pair<ISubSystemPowerProvider, List<IFilter>>> provider = pipe.getRouter().getSubSystemPowerProvider();
		double available = 0;
		outer:
		for (Pair<ISubSystemPowerProvider, List<IFilter>> pair : provider) {
			for (IFilter filter : pair.getValue2()) {
				if (filter.blockPower()) {
					continue outer;
				}
			}
			if (pair.getValue1().usePaused()) {
				continue;
			}
			if (!pair.getValue1().getBrand().equals("RF")) {
				continue;
			}
			available += pair.getValue1().getPowerLevel();
		}
		if (available > 0) {
			double neededPower = PowerSupplierHandler.INTERNAL_RF_BUFFER_MAX - internalBufferRF;
			if (neededPower > 0) {
				if (pipe.useEnergy((int) (neededPower / 100), false)) {
					outer:
					for (Pair<ISubSystemPowerProvider, List<IFilter>> pair : provider) {
						for (IFilter filter : pair.getValue2()) {
							if (filter.blockPower()) {
								continue outer;
							}
						}
						if (pair.getValue1().usePaused()) {
							continue;
						}
						if (!pair.getValue1().getBrand().equals("RF")) {
							continue;
						}
						double requestamount = neededPower * (pair.getValue1().getPowerLevel() / available);
						pair.getValue1().requestPower(pipe.getRouterId(), requestamount);
					}
				}
			}
		}
		return false;
	}

	public void addRFPower(double toSend) {
		internalBufferRF += toSend;
	}

	public void addIC2Power(double toSend) {
		internalBufferIC2 += toSend;
	}
}
