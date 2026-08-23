package logisticspipes.proxy.interfaces;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.nbt.CompoundTag;

/**
 * Forge-energy storage abstraction. Historically wrapped CoFH's EnergyStorage; on 1.20.1 it is
 * implemented by {@link logisticspipes.proxy.PowerProxy} on top of net.minecraftforge.energy.EnergyStorage.
 */
public interface ICoFHEnergyStorage {

	int extractEnergy(int space, boolean b);

	int receiveEnergy(int maxReceive, boolean simulate);

	int getEnergyStored();

	int getMaxEnergyStored();

	void deserialize(ValueInput input);

	void serialize(ValueOutput output);
}
