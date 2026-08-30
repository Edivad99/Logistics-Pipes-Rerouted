package logisticspipes.network.packets.routingdebug;

import net.minecraft.world.entity.player.Player;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.LogisticsPipes;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.debug.ClientViewController;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class RoutingUpdateCanidatePipe extends ModernPacket {

	@Getter
	@Setter
	private ExitRoute exitRoute;

	public RoutingUpdateCanidatePipe(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		// do not handle packet if the ExitRoute could not be read
		if (exitRoute != null) {
			ClientViewController.instance().handlePacket(this);
		}
	}

	@Override
	public ModernPacket template() {
		return new RoutingUpdateCanidatePipe(getId());
	}

	@Override
	public boolean isCompressable() {
		return true;
	}

	@Override
	public void readData(LPDataInput input) {
		try {
			exitRoute = new ExitRoute(input);
		} catch (RuntimeException e) {
			LogisticsPipes.LOG.error("Could not read ExitRoute from RoutingUpdateCanidatePipe", e);
		}
	}

	@Override
	public void writeData(LPDataOutput output) {
		exitRoute.write(output);
	}
}
