package logisticspipes.network.packets.routingdebug;

import net.minecraft.world.entity.player.Player;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.routing.debug.ClientViewController;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class RoutingUpdateInitDebug extends ModernPacket {

	public RoutingUpdateInitDebug(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {}

	@Override
	public void processPacket(Player player) {
		ClientViewController.instance().init(this);
	}

	@Override
	public void writeData(LPDataOutput output) {}

	@Override
	public ModernPacket template() {
		return new RoutingUpdateInitDebug(getId());
	}

	@Override
	public boolean isCompressable() {
		return true;
	}
}
