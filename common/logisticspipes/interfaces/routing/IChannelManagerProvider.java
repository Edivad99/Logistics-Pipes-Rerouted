package logisticspipes.interfaces.routing;

import net.minecraft.world.level.Level;

public interface IChannelManagerProvider {

	IChannelManager getChannelManager(Level world);
}
