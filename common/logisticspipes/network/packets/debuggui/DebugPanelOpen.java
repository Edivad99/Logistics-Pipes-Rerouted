package logisticspipes.network.packets.debuggui;

import logisticspipes.commands.commands.debug.DebugGuiController;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.player.Player;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class DebugPanelOpen extends ModernPacket {

	@Setter
	@Getter
	private String name;

	@Getter
	@Setter
	private int identification;

	public DebugPanelOpen(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new DebugPanelOpen(getId());
	}

	@Override
	public void readData(LPDataInput input) {
		setName(input.readUTF());
		setIdentification(input.readInt());
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeUTF(getName());
		output.writeInt(getIdentification());
	}

	@Override
	public void processPacket(Player player) {
		try {
			DebugGuiController.instance().createNewDebugGui(getName(), getIdentification());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean isCompressable() {
		return true;
	}
}
