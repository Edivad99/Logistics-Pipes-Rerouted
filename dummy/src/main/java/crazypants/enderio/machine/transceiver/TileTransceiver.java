package crazypants.enderio.machine.transceiver;

import java.util.Set;

import net.minecraft.world.level.block.entity.BlockEntity;

public class TileTransceiver extends BlockEntity {

	public Set<Channel> getSendChannels(ChannelType type) {
		return null;
	}

	public Set<Channel> getRecieveChannels(ChannelType type) {
		return null;
	}
}
