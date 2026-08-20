package logisticspipes.security;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import network.rs485.logisticspipes.IStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SecuritySettings implements IStore {

	public @Nullable String name;
	public boolean openGui = false;
	public boolean openRequest = false;
	public boolean openUpgrades = false;
	public boolean openNetworkMonitor = false;
	public boolean removePipes = false;
	public boolean accessRoutingChannels = false;

	public SecuritySettings(@Nullable String name) {
		this.name = name;
	}

	@Override
	public void readFromNBT(CompoundTag tag, HolderLookup.@NotNull Provider provider) {
		String prev = name;
		name = tag.getString("name");
		if (name.isEmpty()) {
			name = prev;
		}
		openGui = tag.getBoolean("openGui");
		openRequest = tag.getBoolean("openRequest");
		openUpgrades = tag.getBoolean("openUpgrades");
		openNetworkMonitor = tag.getBoolean("openNetworkMonitor");
		removePipes = tag.getBoolean("removePipes");
		accessRoutingChannels = tag.getBoolean("accessRoutingChannels");
	}

	@Override
	public void writeToNBT(CompoundTag tag, HolderLookup.@NotNull Provider provider) {
		if (name == null || name.isEmpty()) {
			return;
		}
		tag.putString("name", name);
		tag.putBoolean("openGui", openGui);
		tag.putBoolean("openRequest", openRequest);
		tag.putBoolean("openUpgrades", openUpgrades);
		tag.putBoolean("openNetworkMonitor", openNetworkMonitor);
		tag.putBoolean("removePipes", removePipes);
		tag.putBoolean("accessRoutingChannels", accessRoutingChannels);
	}
}
