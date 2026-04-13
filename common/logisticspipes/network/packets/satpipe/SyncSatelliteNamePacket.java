package logisticspipes.network.packets.satpipe;

import net.minecraft.world.entity.player.Player;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.StringCoordinatesPacket;
import logisticspipes.pipes.PipeFluidSatellite;
import logisticspipes.pipes.PipeItemsSatelliteLogistics;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;
import network.rs485.logisticspipes.SatellitePipe;

@StaticResolve
public class SyncSatelliteNamePacket extends StringCoordinatesPacket {

	public SyncSatelliteNamePacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new SyncSatelliteNamePacket(getId());
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
	}

	@Override
	public void processPacket(Player player) {
		final LogisticsTileGenericPipe pipe = getPipe(player.level(), LTGPCompletionCheck.PIPE);
		if (pipe == null || pipe.pipe == null) {
			return;
		}

		if (pipe.pipe instanceof SatellitePipe) {
			((SatellitePipe) pipe.pipe).setSatellitePipeName(getString());
		}
		
	}
}
