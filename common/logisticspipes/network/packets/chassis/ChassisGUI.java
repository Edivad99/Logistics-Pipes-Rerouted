package logisticspipes.network.packets.chassis;

import net.minecraft.world.entity.player.Player;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.module.Gui;

@StaticResolve
public class ChassisGUI extends CoordinatesPacket {

	@Getter
	@Setter
	private int buttonID;

	public ChassisGUI(int id) {
		super(id);
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeInt(buttonID);
		super.writeData(output);
	}

	@Override
	public void readData(LPDataInput input) {
		buttonID = input.readInt();
		super.readData(input);
	}

	@Override
	public void processPacket(Player player) {
		final LogisticsTileGenericPipe pipe = getPipe(player.level(), LTGPCompletionCheck.PIPE);
		if (pipe.pipe instanceof PipeLogisticsChassis) {
			LogisticsModule subModule = ((PipeLogisticsChassis) pipe.pipe).getSubModule(getButtonID());
			if (subModule instanceof Gui) {
				Gui.getPipeGuiProvider((Gui) subModule).setPosX(getPosX()).setPosY(getPosY()).setPosZ(getPosZ()).open(player);
			}
		}
	}

	@Override
	public ModernPacket template() {
		return new ChassisGUI(getId());
	}

}
