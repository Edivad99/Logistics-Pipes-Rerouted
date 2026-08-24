package logisticspipes.security;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.common.util.ValueIOSerializable;

import org.jspecify.annotations.Nullable;

public class SecuritySettings implements ValueIOSerializable {

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
	public void deserialize(ValueInput input) {
		String prev = name;
		name = input.getStringOr("name", "");
		if (name.isEmpty()) {
			name = prev;
		}
		openGui = input.getBooleanOr("openGui", false);
		openRequest = input.getBooleanOr("openRequest", false);
		openUpgrades = input.getBooleanOr("openUpgrades", false);
		openNetworkMonitor = input.getBooleanOr("openNetworkMonitor", false);
		removePipes = input.getBooleanOr("removePipes", false);
		accessRoutingChannels = input.getBooleanOr("accessRoutingChannels", false);
	}

	@Override
	public void serialize(ValueOutput output) {
		if (name == null || name.isEmpty()) {
			return;
		}
		output.putString("name", name);
		output.putBoolean("openGui", openGui);
		output.putBoolean("openRequest", openRequest);
		output.putBoolean("openUpgrades", openUpgrades);
		output.putBoolean("openNetworkMonitor", openNetworkMonitor);
		output.putBoolean("removePipes", removePipes);
		output.putBoolean("accessRoutingChannels", accessRoutingChannels);
	}
}
