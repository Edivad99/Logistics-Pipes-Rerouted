package logisticspipes.security;

import javax.annotation.Nonnull;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import network.rs485.logisticspipes.IStore;
import org.jetbrains.annotations.NotNull;

public class SecuritySettings implements IStore {

	public String name;
	public boolean openGui = false;
	public boolean openRequest = false;
	public boolean openUpgrades = false;
	public boolean openNetworkMonitor = false;
	public boolean removePipes = false;
	public boolean accessRoutingChannels = false;

	public SecuritySettings(String name) {
		this.name = name;
	}

	@Override
	public void readFromNBT(@Nonnull CompoundTag nbttagcompound, HolderLookup.@NotNull Provider provider) {
		String prev = name;
		name = nbttagcompound.getString("name");
		if (name.equals("")) {
			name = prev;
		}
		openGui = nbttagcompound.getBoolean("openGui");
		openRequest = nbttagcompound.getBoolean("openRequest");
		openUpgrades = nbttagcompound.getBoolean("openUpgrades");
		openNetworkMonitor = nbttagcompound.getBoolean("openNetworkMonitor");
		removePipes = nbttagcompound.getBoolean("removePipes");
		accessRoutingChannels = nbttagcompound.getBoolean("accessRoutingChannels");
	}

	@Override
	public void writeToNBT(@Nonnull CompoundTag nbttagcompound, HolderLookup.@NotNull Provider provider) {
		if (name == null || name.isEmpty()) {
			return;
		}
		nbttagcompound.putString("name", name);
		nbttagcompound.putBoolean("openGui", openGui);
		nbttagcompound.putBoolean("openRequest", openRequest);
		nbttagcompound.putBoolean("openUpgrades", openUpgrades);
		nbttagcompound.putBoolean("openNetworkMonitor", openNetworkMonitor);
		nbttagcompound.putBoolean("removePipes", removePipes);
		nbttagcompound.putBoolean("accessRoutingChannels", accessRoutingChannels);
	}
}
