package logisticspipes.proxy.interfaces;

import net.minecraft.core.Direction;

/**
 * Forge-energy receiver abstraction. Historically wrapped CoFH's IEnergyReceiver; on 1.20.1 it is
 * implemented by {@link logisticspipes.proxy.PowerProxy} on top of ForgeCapabilities.ENERGY.
 */
public interface ICoFHEnergyReceiver {

	int getMaxEnergyStored();

	int getEnergyStored();

	int receiveEnergy(Direction opposite, int i, boolean b);

}
