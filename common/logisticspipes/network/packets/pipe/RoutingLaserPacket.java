package logisticspipes.network.packets.pipe;

import java.util.ArrayList;
import java.util.List;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.renderer.LogisticsHUDRenderer;
import logisticspipes.routing.LaserData;
import logisticspipes.utils.StaticResolve;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.player.Player;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class RoutingLaserPacket extends ModernPacket {

	@Setter
	@Getter
	List<LaserData> lasers = new ArrayList<>();

	public RoutingLaserPacket(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {
		while (input.readBoolean()) {
			lasers.add(new LaserData(input));
		}
	}

	@Override
	public void processPacket(Player player) {
		LogisticsHUDRenderer.instance().setLasers(getLasers());
	}

	@Override
	public void writeData(LPDataOutput output) {
		for (LaserData laser : lasers) {
			output.writeBoolean(true);
			laser.writeData(output);
		}
		output.writeBoolean(false);
	}

	@Override
	public ModernPacket template() {
		return new RoutingLaserPacket(getId());
	}

}
