package logisticspipes.network.packets.gui;

import logisticspipes.LogisticsPipes;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.player.Player;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class GuiReopenPacket extends CoordinatesPacket {

	@Getter
	@Setter
	private int guiID;

	public GuiReopenPacket(int id) {
		super(id);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeInt(getGuiID());
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		guiID = input.readInt();
	}

	@Override
	public void processPacket(Player player) {
		if (NewGuiHandler.guilist == null || guiID < 0 || guiID >= NewGuiHandler.guilist.size()) {
			LogisticsPipes.LOG.warn("GuiReopenPacket: invalid guiID {}", guiID);
			return;
		}
		GuiProvider provider = NewGuiHandler.guilist.get(guiID).template();
		if (provider instanceof CoordinatesGuiProvider coord) {
			coord.setPosX(getPosX());
			coord.setPosY(getPosY());
			coord.setPosZ(getPosZ());
		}
		provider.open(player);
	}

	@Override
	public ModernPacket template() {
		return new GuiReopenPacket(getId());
	}

}
