package logisticspipes.network.packets.debug;

import net.minecraft.world.entity.player.Player;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.debug.LogWindow;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class SendNewLogWindow extends ModernPacket {

	@Getter
	@Setter
	private int windowID;

	@Getter
	@Setter
	private String title;

	public SendNewLogWindow(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {
		windowID = input.readInt();
		title = input.readUTF();
	}

	@Override
	public void processPacket(Player player) {
		LogWindow.getWindow(windowID).setTitle(title);
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeInt(windowID);
		output.writeUTF(title);
	}

	@Override
	public ModernPacket template() {
		return new SendNewLogWindow(getId());
	}

	@Override
	public boolean isCompressable() {
		return true;
	}
}
